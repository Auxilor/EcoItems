package com.willfp.ecoitems.furniture

import com.willfp.eco.core.integrations.antigrief.AntigriefManager
import com.willfp.ecoitems.blocks.BlockListener
import com.willfp.ecoitems.items.ecoItem
import com.willfp.ecoitems.libreforge.ContentEvent
import com.willfp.ecoitems.plugin
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.SoundCategory
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.util.BoundingBox
import kotlin.math.roundToInt

object FurnitureListener : Listener {
    /** True while dispatching our own synthetic place/break events. */
    private var mutating = false

    // Not ignoreCancelled: denied block use (custom blocks/furniture) still
    // allows item use, which is what placement is.
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlace(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK || event.hand != EquipmentSlot.HAND) {
            return
        }
        if (event.useItemInHand() == org.bukkit.event.Event.Result.DENY) {
            return
        }

        val item = event.item ?: return
        val ecoItem = item.ecoItem ?: return
        val furniture = ecoItem.furniture ?: return
        val against = event.clickedBlock ?: return
        val player = event.player

        if (!player.isSneaking && against.type.isInteractable &&
            com.willfp.ecoitems.blocks.EcoBlocks.at(against) == null
        ) {
            return
        }

        val face = event.blockFace
        val allowed = when (face) {
            BlockFace.UP -> furniture.floor
            BlockFace.DOWN -> furniture.ceiling
            else -> furniture.wall
        }
        if (!allowed) {
            return
        }

        val target = if (isReplaceable(against)) against else against.getRelative(face)
        if (!isReplaceable(target)) {
            return
        }

        event.setCancelled(true)

        val yaw = when (face) {
            // Player yaw directly makes the model front face the player.
            BlockFace.UP, BlockFace.DOWN -> snapYaw(player.location.yaw, furniture.effectiveRotationStep)
            else -> faceYaw(face)
        }

        // All collision cells need to be free.
        val origin = target
        val blocked = furniture.barriers.any { cell ->
            val (x, z) = PlacedFurniture.rotate(cell.x.toDouble(), cell.z.toDouble(), yaw)
            val block = origin.getRelative(x.roundToInt(), cell.y, z.roundToInt())
            !isReplaceable(block) || player.boundingBox.overlaps(BoundingBox.of(block))
        }
        if (blocked) {
            return
        }

        if (!AntigriefManager.canPlaceBlock(player, target)) {
            return
        }

        if (!BlockListener.passesPlacementCooldown(player)) {
            return
        }

        val placeEvent = BlockPlaceEvent(target, target.state, against, item, player, true, event.hand!!)
        mutating = true
        try {
            plugin.server.pluginManager.callEvent(placeEvent)
        } finally {
            mutating = false
        }
        if (placeEvent.isCancelled || !placeEvent.canBuild()) {
            return
        }

        PlacedFurniture.place(ecoItem, furniture, origin, yaw, 0f)

        if (player.gameMode != GameMode.CREATIVE) {
            item.amount -= 1
        }
        player.swingMainHand()
        furniture.effects.dispatch(ContentEvent.PLACE, player, origin.location.add(0.5, 0.5, 0.5))
        playSound(origin, furniture, furniture.sounds?.place ?: "minecraft:block.wood.place")
    }

    /** Attacking the furniture (via its interaction hitbox) breaks it. */
    @EventHandler(ignoreCancelled = true)
    fun onAttack(event: EntityDamageByEntityEvent) {
        val placed = PlacedFurniture.fromEntity(event.entity) ?: return
        event.isCancelled = true

        val player = event.damager as? Player ?: return
        placed.furniture?.effects?.dispatch(
            if (player.isSneaking) ContentEvent.SHIFT_PUNCH else ContentEvent.PUNCH,
            player,
            placed.base.location
        )
        breakFurniture(placed, player)
    }

    /**
     * Barriers are unbreakable in survival, so mining never progresses -
     * insta-break them when they belong to furniture. The break then lands
     * in onBarrierBreak below.
     */
    @EventHandler(ignoreCancelled = true)
    fun onBarrierDamage(event: org.bukkit.event.block.BlockDamageEvent) {
        val placed = PlacedFurniture.atBarrier(event.block) ?: return
        placed.furniture?.effects?.dispatch(
            if (event.player.isSneaking) ContentEvent.SHIFT_PUNCH else ContentEvent.PUNCH,
            event.player,
            placed.base.location
        )
        event.instaBreak = true
    }

    /** Breaking a collision barrier breaks the furniture. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBarrierBreak(event: BlockBreakEvent) {
        if (mutating) {
            return
        }

        val placed = PlacedFurniture.atBarrier(event.block) ?: return
        event.isCancelled = true
        breakFurniture(placed, event.player)
    }

    private fun breakFurniture(placed: PlacedFurniture, player: Player) {
        val block = placed.base.location.block

        if (!AntigriefManager.canBreakBlock(player, block)) {
            return
        }

        val breakEvent = BlockBreakEvent(block, player)
        mutating = true
        try {
            plugin.server.pluginManager.callEvent(breakEvent)
        } finally {
            mutating = false
        }
        if (breakEvent.isCancelled) {
            return
        }

        placed.furniture?.effects?.dispatch(ContentEvent.BREAK, player, placed.base.location)
        playSound(block, placed.furniture, placed.furniture?.sounds?.breakSound ?: "minecraft:block.wood.break")
        placed.remove(player, drop = true)
    }

    @EventHandler
    fun onInteractEntity(event: PlayerInteractEntityEvent) {
        if (event.hand != EquipmentSlot.HAND) {
            return
        }

        val placed = PlacedFurniture.fromEntity(event.rightClicked) ?: return
        event.setCancelled(true)
        handleInteract(placed, event.player)
    }

    @EventHandler(ignoreCancelled = true)
    fun onInteractBarrier(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK || event.hand != EquipmentSlot.HAND) {
            return
        }

        val block = event.clickedBlock ?: return
        if (block.type != Material.BARRIER || event.player.isSneaking) {
            return
        }

        val placed = PlacedFurniture.atBarrier(block) ?: return
        event.setCancelled(true)
        handleInteract(placed, event.player)
    }

    private fun handleInteract(placed: PlacedFurniture, player: Player) {
        val effects = placed.furniture?.effects

        if (player.isSneaking) {
            effects?.dispatch(ContentEvent.SHIFT_RIGHT_CLICK, player, placed.base.location)
            return
        }

        effects?.dispatch(ContentEvent.RIGHT_CLICK, player, placed.base.location)

        if (placed.sit(player)) {
            effects?.dispatch(ContentEvent.SIT, player, placed.base.location)
            return
        }

        val furniture = placed.furniture
        if (furniture?.toggleableLights == true) {
            placed.toggleLights()
        }

        if (furniture != null && furniture.states.isNotEmpty() && furniture.cycleStatesOnClick) {
            placed.cycleState()
        }
    }

    private fun snapYaw(yaw: Float, step: Int): Float {
        val normalized = ((yaw % 360) + 360) % 360
        if (step == 0) {
            return normalized
        }
        return ((normalized / step).roundToInt() * step % 360).toFloat()
    }

    private fun faceYaw(face: BlockFace): Float = when (face) {
        BlockFace.NORTH -> 180f
        BlockFace.EAST -> 270f
        BlockFace.SOUTH -> 0f
        BlockFace.WEST -> 90f
        else -> 0f
    }

    private fun isReplaceable(block: Block): Boolean =
        block.type.isAir || block.isLiquid || REPLACEABLE.contains(block.type)

    private val REPLACEABLE = setOf(
        Material.SHORT_GRASS, Material.TALL_GRASS, Material.FERN, Material.LARGE_FERN,
        Material.SEAGRASS, Material.TALL_SEAGRASS, Material.SNOW, Material.VINE,
        Material.DEAD_BUSH, Material.LIGHT, Material.FIRE, Material.SOUL_FIRE
    )

    private fun playSound(block: Block, furniture: Furniture?, sound: String) {
        block.world.playSound(
            block.location.add(0.5, 0.5, 0.5),
            BlockListener.remapSilencedSound(sound),
            SoundCategory.BLOCKS,
            furniture?.sounds?.volume?.toFloat() ?: 1.0f,
            furniture?.sounds?.pitch?.toFloat() ?: 0.8f
        )
    }
}
