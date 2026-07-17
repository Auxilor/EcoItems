package com.willfp.ecoitems.furniture

import com.willfp.ecoitems.blocks.BlockListener
import com.willfp.ecoitems.items.EcoItem
import com.willfp.ecoitems.items.EcoItems
import com.willfp.ecoitems.nms.ItemComponentsProxy
import com.willfp.ecoitems.plugin
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.data.type.Light as LightData
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Display
import org.bukkit.entity.Entity
import org.bukkit.entity.Interaction
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.BoundingBox
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.UUID
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A furniture instance in the world: a persistent ItemDisplay carrying the
 * furniture id plus the placed cell/entity references in its PDC. Everything
 * else (barriers, interaction hitboxes, seats, lights) is resolved from it.
 */
class PlacedFurniture(
    val base: ItemDisplay,
    val id: String
) {
    val item: EcoItem? get() = EcoItems.getByID(id)
    val furniture: Furniture? get() = item?.furniture

    private val origin: Block get() = base.location.block

    private fun cells(key: NamespacedKey): List<Block> =
        base.persistentDataContainer.get(key, PersistentDataType.STRING)
            ?.split(";")
            ?.mapNotNull { entry ->
                val parts = entry.split(",").mapNotNull { it.toIntOrNull() }
                if (parts.size == 3) origin.getRelative(parts[0], parts[1], parts[2]) else null
            }
            .orEmpty()

    private fun entities(key: NamespacedKey): List<Entity> =
        base.persistentDataContainer.get(key, PersistentDataType.STRING)
            ?.split(";")
            ?.mapNotNull { raw -> runCatching { Bukkit.getEntity(UUID.fromString(raw)) }.getOrNull() }
            .orEmpty()

    fun barrierBlocks(): List<Block> = cells(BARRIERS)
    fun lightBlocks(): List<Block> = cells(LIGHTS)
    fun seatEntities(): List<ArmorStand> = entities(SEATS).filterIsInstance<ArmorStand>()

    /** Removes the furniture and everything it placed. */
    fun remove(player: Player?, drop: Boolean) {
        // Shared storage contents always drop, like a broken chest.
        if (furniture?.storage?.type == StorageType.STORAGE) {
            val contents = FurnitureStorageManager.takeContents(this)
            contents.forEach { base.world.dropItemNaturally(base.location, it) }
        }

        if (furniture?.connectable != null) {
            FurnitureConnections.updateNeighborsAfterRemoval(this)
        }

        for (barrier in barrierBlocks()) {
            if (barrier.type == Material.BARRIER) {
                barrier.type = Material.AIR
            }
        }

        for (light in lightBlocks()) {
            if (light.type == Material.LIGHT) {
                light.type = Material.AIR
            }
        }

        for (seat in seatEntities()) {
            seat.passengers.forEach(seat::removePassenger)
            seat.remove()
        }
        entities(INTERACTIONS).forEach(Entity::remove)

        val location = base.location
        base.remove()

        if (drop && player?.gameMode != org.bukkit.GameMode.CREATIVE) {
            val item = this.item
            val furniture = this.furniture
            if (item != null) {
                dropFurnitureItems(furniture, item, location, player)
            }
        }
    }

    /** Toggles the light blocks between their level and off. */
    fun toggleLights() {
        val furniture = this.furniture ?: return
        val lit = lightBlocks().any { it.type == Material.LIGHT }

        if (lit) {
            lightBlocks().forEach { if (it.type == Material.LIGHT) it.type = Material.AIR }
            return
        }

        // The stored cells parallel the config's light list, in order.
        for ((block, light) in lightBlocks().zip(furniture.lights)) {
            placeLight(block, light.level)
        }
    }

    /** The current state name, or null when the furniture has none. */
    fun state(): String? =
        base.persistentDataContainer.get(STATE, PersistentDataType.STRING)

    /** Switches the displayed model to a named state. */
    fun setState(name: String) {
        val furniture = this.furniture ?: return
        val state = furniture.states[name] ?: return

        base.persistentDataContainer.set(STATE, PersistentDataType.STRING, name)
        applyModel(state.modelKey)
    }

    /** Shows an alternative item_model on the display (null = the item's own look). */
    internal fun applyModel(modelKey: String?) {
        val stack = item?.itemStack ?: return
        base.setItemStack(modelKey?.let { model ->
            plugin.getProxy(ItemComponentsProxy::class.java)
                .withComponents(stack, mapOf("minecraft:item_model" to model)).item
        } ?: stack)
    }

    /** Advances to the next configured state, wrapping around. */
    fun cycleState() {
        val furniture = this.furniture ?: return
        val names = furniture.states.keys.toList()
        if (names.isEmpty()) {
            return
        }

        val current = state() ?: furniture.defaultState
        setState(names[(names.indexOf(current) + 1).mod(names.size)])
    }

    fun isDoorOpen(): Boolean =
        base.persistentDataContainer.get(DOOR_OPEN, PersistentDataType.BYTE) == 1.toByte()

    /** Opens or closes a door: toggles barrier collision and the look. */
    fun toggleDoor(door: FurnitureDoor) {
        val open = !isDoorOpen()
        base.persistentDataContainer.set(DOOR_OPEN, PersistentDataType.BYTE, if (open) 1 else 0)

        for (barrier in barrierBlocks()) {
            if (open) {
                if (barrier.type == Material.BARRIER) {
                    barrier.type = Material.AIR
                }
            } else if (barrier.type.isAir || barrier.isLiquid) {
                barrier.type = Material.BARRIER
            }
        }

        applyModel(if (open) door.openState?.modelKey else null)

        base.world.playSound(
            base.location,
            if (open) door.openSound else door.closeSound,
            org.bukkit.SoundCategory.BLOCKS,
            1.0f,
            1.0f
        )
    }

    /** Seats the player on the nearest free seat, if any. */
    fun sit(player: Player): Boolean {
        val seat = seatEntities()
            .filter { it.passengers.isEmpty() }
            .minByOrNull { it.location.distanceSquared(player.location) }
            ?: return false

        // Mounting mid-interact-packet desyncs the client; next tick is safe.
        com.willfp.ecoitems.plugin.scheduler.run {
            if (seat.isValid && seat.passengers.isEmpty()) {
                seat.addPassenger(player)
            }
        }
        return true
    }

    companion object {
        private val ID = NamespacedKey(plugin, "furniture")
        private val BASE = NamespacedKey(plugin, "furniture-base")
        private val BARRIERS = NamespacedKey(plugin, "furniture-barriers")
        private val LIGHTS = NamespacedKey(plugin, "furniture-lights")
        private val SEATS = NamespacedKey(plugin, "furniture-seats")
        private val INTERACTIONS = NamespacedKey(plugin, "furniture-interactions")
        private val STATE = NamespacedKey(plugin, "furniture-state")
        private val DOOR_OPEN = NamespacedKey(plugin, "furniture-door-open")

        /** Resolve from any furniture entity: base, hitbox, or seat. */
        fun fromEntity(entity: Entity?): PlacedFurniture? {
            if (entity == null) return null

            val base = if (entity is ItemDisplay && entity.persistentDataContainer.has(ID, PersistentDataType.STRING)) {
                entity
            } else {
                entity.persistentDataContainer.get(BASE, PersistentDataType.STRING)
                    ?.let { runCatching { Bukkit.getEntity(UUID.fromString(it)) }.getOrNull() } as? ItemDisplay
                    ?: return null
            }

            val id = base.persistentDataContainer.get(ID, PersistentDataType.STRING) ?: return null
            return PlacedFurniture(base, id)
        }

        /** Resolve from a barrier block one of the placed cells owns. */
        fun atBarrier(block: Block): PlacedFurniture? {
            if (block.type != Material.BARRIER) return null

            val box = BoundingBox.of(block).expand(SEARCH_RADIUS)
            for (entity in block.world.getNearbyEntities(box) { it is ItemDisplay }) {
                val placed = fromEntity(entity) ?: continue
                if (placed.barrierBlocks().any { it.x == block.x && it.y == block.y && it.z == block.z }) {
                    return placed
                }
            }

            return null
        }

        private const val SEARCH_RADIUS = 8.0

        // Seat offsets follow the Nexo/Oraxen convention: y = 0 is the
        // natural sitting height on a chair-height cushion (~0.6 above the
        // block bottom), so seat vectors from imported setups work verbatim.
        private const val SIT_HEIGHT = 0.6

        /**
         * Spawns the furniture at the origin block. The caller has already
         * checked space and protection.
         */
        fun place(item: EcoItem, furniture: Furniture, origin: Block, yaw: Float, pitch: Float): PlacedFurniture {
            val world = origin.world
            // Offsets (seats, hitboxes, lights) are relative to the block
            // bottom; the display itself spawns at the block center, since
            // item displays render their model centered on the entity.
            val center = origin.location.add(0.5, 0.0, 0.5)
            val displayLocation = origin.location.add(0.5, 0.5, 0.5)

            val barrierCells = furniture.barriers.map { cell ->
                val (x, z) = rotate(cell.x.toDouble(), cell.z.toDouble(), yaw)
                origin.getRelative(x.roundToInt(), cell.y, z.roundToInt())
            }
            val lightCells = furniture.lights.map { light ->
                val (x, z) = rotate(light.x.toDouble(), light.z.toDouble(), yaw)
                origin.getRelative(x.roundToInt(), light.y, z.roundToInt()) to light.level
            }

            val base = world.spawn(displayLocation, ItemDisplay::class.java) { display ->
                display.setItemStack(item.itemStack)
                display.itemDisplayTransform = furniture.transform
                display.billboard = furniture.billboard
                furniture.brightness?.let { display.brightness = Display.Brightness(it, 15) }
                furniture.viewRange?.let { display.viewRange = it.toFloat() }
                display.isPersistent = true
                // Item displays render their model rotated 180 on Y versus
                // block space; compensating here keeps barrier/seat offsets
                // aligned with the model as it was designed.
                display.setRotation(yaw + 180f, pitch)

                if (furniture.scale != null || furniture.translation != null) {
                    val scale = furniture.scale ?: Triple(1.0, 1.0, 1.0)
                    val translation = furniture.translation ?: Triple(0.0, 0.0, 0.0)
                    display.transformation = org.bukkit.util.Transformation(
                        Vector3f(translation.first.toFloat(), translation.second.toFloat(), translation.third.toFloat()),
                        Quaternionf(),
                        Vector3f(scale.first.toFloat(), scale.second.toFloat(), scale.third.toFloat()),
                        Quaternionf()
                    )
                }

                val pdc = display.persistentDataContainer
                pdc.set(ID, PersistentDataType.STRING, furniture.id)
                pdc.set(BARRIERS, PersistentDataType.STRING, barrierCells.joinToString(";") {
                    "${it.x - origin.x},${it.y - origin.y},${it.z - origin.z}"
                })
                pdc.set(LIGHTS, PersistentDataType.STRING, lightCells.joinToString(";") {
                    "${it.first.x - origin.x},${it.first.y - origin.y},${it.first.z - origin.z}"
                })
            }

            val interactions = furniture.hitboxes.map { hitbox ->
                val (x, z) = rotate(hitbox.x, hitbox.z, yaw)
                world.spawn(
                    center.clone().add(x, hitbox.y, z),
                    Interaction::class.java
                ) { interaction ->
                    interaction.interactionWidth = hitbox.width.toFloat()
                    interaction.interactionHeight = hitbox.height.toFloat()
                    interaction.isPersistent = true
                    interaction.persistentDataContainer.set(ID, PersistentDataType.STRING, furniture.id)
                }
            }

            val seats = furniture.seats.map { seat ->
                val (x, z) = rotate(seat.x, seat.z, yaw)
                world.spawn(
                    center.clone().add(x, seat.y + SIT_HEIGHT, z),
                    ArmorStand::class.java
                ) { stand ->
                    // Markers have a zero-height hitbox: passengers sit at the
                    // stand's exact position, clicks pass through to the
                    // furniture, and nothing pokes out of the collision box.
                    stand.isMarker = true
                    stand.isVisible = false
                    stand.isSmall = true
                    stand.setGravity(false)
                    stand.isInvulnerable = true
                    stand.isSilent = true
                    stand.isCollidable = false
                    stand.isPersistent = true
                    stand.setRotation(seat.yaw ?: yaw, 0f)
                    stand.persistentDataContainer.set(ID, PersistentDataType.STRING, furniture.id)
                }
            }

            // Back-references need the base uuid, which only exists after spawn.
            (interactions + seats).forEach {
                it.persistentDataContainer.set(BASE, PersistentDataType.STRING, base.uniqueId.toString())
            }
            base.persistentDataContainer.set(
                INTERACTIONS, PersistentDataType.STRING,
                interactions.joinToString(";") { it.uniqueId.toString() }
            )
            base.persistentDataContainer.set(
                SEATS, PersistentDataType.STRING,
                seats.joinToString(";") { it.uniqueId.toString() }
            )

            for (cell in barrierCells) {
                cell.type = Material.BARRIER
            }
            for ((cell, level) in lightCells) {
                placeLight(cell, level)
            }

            val placed = PlacedFurniture(base, furniture.id)
            furniture.defaultState?.let { placed.setState(it) }
            return placed
        }

        private fun placeLight(block: Block, level: Int) {
            if (!block.type.isAir && block.type != Material.LIGHT) {
                return
            }

            val data = Material.LIGHT.createBlockData() as LightData
            data.level = level
            block.blockData = data
        }

        /**
         * Rotates an offset to match how the client rotates the display
         * model for the entity's yaw (verified in game - the 90/270 cases
         * are the ones that expose a wrong sign here).
         */
        internal fun rotate(x: Double, z: Double, yaw: Float): Pair<Double, Double> {
            val radians = Math.toRadians(yaw.toDouble())
            return (x * cos(radians) - z * sin(radians)) to (x * sin(radians) + z * cos(radians))
        }

        internal fun dropFurnitureItems(
            furniture: Furniture?,
            item: EcoItem,
            location: Location,
            player: Player?
        ) {
            BlockListener.spawnDrops(
                furniture?.drops,
                item.itemStack,
                location,
                player?.inventory?.itemInMainHand,
                player
            ) { plugin.logger.warning("Furniture ${furniture?.id} drop '$it' is not a valid item") }
        }
    }
}
