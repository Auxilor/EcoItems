package com.willfp.ecoitems.furniture

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecoitems.blocks.BlockDrops
import com.willfp.ecoitems.blocks.BlockSounds
import com.willfp.ecoitems.libreforge.ContentEffects
import com.willfp.ecoitems.plugin
import org.bukkit.entity.Display
import org.bukkit.entity.ItemDisplay

/**
 * The furniture half of an item config's `furniture:` section. Furniture is
 * an ItemDisplay showing the item itself, plus optional barrier collision,
 * interaction hitboxes, seats, and light blocks.
 */
class Furniture(val id: String, val config: Config) {
    /** Yaw snapping: 45 (8-way, default), 90 (4-way), or 0 (free). */
    val rotationStep = when (config.getString("rotation").lowercase()) {
        "", "8-way", "8" -> 45
        "4-way", "4" -> 90
        "none", "free" -> 0
        else -> {
            plugin.logger.warning("Furniture $id has unknown rotation '${config.getString("rotation")}' (8-way, 4-way, none)")
            45
        }
    }

    val floor = config.getBoolOrNull("placement.floor") ?: true
    val wall = config.getBoolOrNull("placement.wall") ?: false
    val ceiling = config.getBoolOrNull("placement.ceiling") ?: false

    /** Driveable furniture: no collision/light blocks, moves with the driver. */
    val vehicle = if (config.has("vehicle")) FurnitureVehicle(config.getSubsection("vehicle")) else null

    /** Solid collision cells, relative to the origin block. */
    val barriers: List<Cell> = config.getStrings("barriers").flatMap { parseCells(it) }
        .let { cells ->
            if (vehicle != null && cells.isNotEmpty()) {
                plugin.logger.warning("Furniture $id is a vehicle; barriers are ignored (they can't move)")
                emptyList()
            } else {
                cells
            }
        }

    /**
     * Click hitboxes: "x,y,z" or "x,y,z widthxheight". Defaults to a single
     * 1x1 box at the origin when there are no barriers to click instead.
     */
    val hitboxes: List<Hitbox> = config.getStrings("hitboxes").mapNotNull { parseHitbox(it) }
        .ifEmpty { if (barriers.isEmpty()) listOf(Hitbox(0.0, 0.0, 0.0, 1.0, 1.0)) else emptyList() }

    /** Seat offsets: "x,y,z" with an optional yaw ("x,y,z 90"); y = 0 sits at natural chair height. */
    val seats: List<Seat> = config.getStrings("seats").mapNotNull { parseSeat(it) }

    /** Bed cells: "x,y,z" with an optional yaw; right-click at night to sleep. */
    val beds: List<Seat> = config.getStrings("beds").mapNotNull { parseSeat(it) }

    /** Light cells: "x,y,z level". */
    val lights: List<Light> = config.getStrings("lights").mapNotNull { parseLight(it) }
        .let { lights ->
            if (vehicle != null && lights.isNotEmpty()) {
                plugin.logger.warning("Furniture $id is a vehicle; lights are ignored (they can't move)")
                emptyList()
            } else {
                lights
            }
        }

    /** Right-clicking toggles the lights on and off. */
    val toggleableLights = config.getBool("toggleable-lights")

    /** Named looks the furniture can switch between (lamps on/off, machines...). */
    val states: Map<String, FurnitureState> = if (config.has("states")) {
        val section = config.getSubsection("states")
        section.getKeys(false).associateWith { state ->
            FurnitureState(id, state, section.getSubsection(state))
        }
    } else {
        emptyMap()
    }

    /** The state a fresh placement starts in. */
    val defaultState = config.getStringOrNull("default-state")?.takeIf { it in states }
        ?: states.keys.firstOrNull()

    /** Right-clicking cycles through the states (sitting takes precedence). */
    val cycleStatesOnClick = config.getBoolOrNull("cycle-states-on-click") ?: true

    /** Door behaviour: right-click opens/closes, toggling barrier collision. */
    val door = if (config.has("door")) FurnitureDoor(id, config.getSubsection("door")) else null

    /** Chest-style storage opened on right-click. */
    val storage = if (config.has("storage")) FurnitureStorage(id, config.getSubsection("storage")) else null

    /** Auto-connecting rows (benches, counters, curtains). */
    val connectable = if (config.has("connectable")) {
        FurnitureConnectable(id, config.getSubsection("connectable"))
    } else {
        null
    }

    /** null = drop the placer item itself. */
    val drops = if (config.has("drops")) BlockDrops(id, config.getSubsection("drops")) else null

    val sounds = if (config.has("sounds")) BlockSounds(config.getSubsection("sounds")) else null

    /** Effects run when players interact with the placed furniture. */
    val effects = ContentEffects("Furniture $id", config)

    val scale = config.getStringOrNull("display.scale")?.let { raw ->
        val parts = raw.split(",").mapNotNull { it.trim().toDoubleOrNull() }
        when (parts.size) {
            1 -> Triple(parts[0], parts[0], parts[0])
            3 -> Triple(parts[0], parts[1], parts[2])
            else -> {
                plugin.logger.warning("Furniture $id has invalid display.scale '$raw'")
                null
            }
        }
    }

    val translation = config.getStringOrNull("display.translation")?.let { raw ->
        val parts = raw.split(",").mapNotNull { it.trim().toDoubleOrNull() }
        if (parts.size == 3) Triple(parts[0], parts[1], parts[2]) else {
            plugin.logger.warning("Furniture $id has invalid display.translation '$raw'")
            null
        }
    }

    val transform = config.getStringOrNull("display.transform")?.let { raw ->
        runCatching { ItemDisplay.ItemDisplayTransform.valueOf(raw.uppercase()) }.getOrElse {
            plugin.logger.warning("Furniture $id has unknown display.transform '$raw'")
            null
        }
    } ?: ItemDisplay.ItemDisplayTransform.NONE

    val billboard = config.getStringOrNull("display.billboard")?.let { raw ->
        runCatching { Display.Billboard.valueOf(raw.uppercase()) }.getOrElse {
            plugin.logger.warning("Furniture $id has unknown display.billboard '$raw'")
            null
        }
    } ?: Display.Billboard.FIXED

    /** Fixed block-light level for the display, 0-15; unset = world lighting. */
    val brightness = config.getIntOrNull("display.brightness")?.coerceIn(0, 15)

    val viewRange = config.getDoubleOrNull("display.view-range")

    /** Multi-cell collision only rotates in quarter turns. */
    val effectiveRotationStep: Int
        get() = if (barriers.any { it.x != 0 || it.z != 0 } && rotationStep != 0) 90 else rotationStep

    /** Every alternative look this furniture can show (for pack generation). */
    val stateModels: List<FurnitureState>
        get() = states.values + listOfNotNull(door?.openState) + (connectable?.all ?: emptyList())

    init {
        if (com.willfp.ecoitems.BuildConfig.FREE_VERSION && stateModels.any { it.hasAssets }) {
            plugin.logger.warning(
                "Furniture $id has state models, but state models require the paid version of EcoItems"
            )
        }
    }

    data class Cell(val x: Int, val y: Int, val z: Int)
    data class Hitbox(val x: Double, val y: Double, val z: Double, val width: Double, val height: Double)
    data class Seat(val x: Double, val y: Double, val z: Double, val yaw: Float?)
    data class Light(val x: Int, val y: Int, val z: Int, val level: Int)

    /** "x,y,z" with ranges: "0..1,0,2" expands along each axis. */
    private fun parseCells(raw: String): List<Cell> {
        val axes = raw.split(",").map { part ->
            val trimmed = part.trim()
            if (".." in trimmed) {
                val (from, to) = trimmed.split("..", limit = 2).map { it.trim().toIntOrNull() }
                if (from == null || to == null) null else (minOf(from, to)..maxOf(from, to)).toList()
            } else {
                trimmed.toIntOrNull()?.let { listOf(it) }
            }
        }

        if (axes.size != 3 || axes.any { it == null }) {
            plugin.logger.warning("Furniture $id has invalid barrier cell '$raw' (use \"x,y,z\", ranges like 0..1 allowed)")
            return emptyList()
        }

        val (xs, ys, zs) = axes.map { it!! }
        return xs.flatMap { x -> ys.flatMap { y -> zs.map { z -> Cell(x, y, z) } } }
    }

    private fun parseHitbox(raw: String): Hitbox? {
        val parts = raw.trim().split(" ", limit = 2)
        val coords = parts[0].split(",").mapNotNull { it.trim().toDoubleOrNull() }
        if (coords.size != 3) {
            plugin.logger.warning("Furniture $id has invalid hitbox '$raw' (use \"x,y,z widthxheight\")")
            return null
        }

        val size = parts.getOrNull(1)?.split("x", ",")?.mapNotNull { it.trim().toDoubleOrNull() }
        val width = size?.getOrNull(0) ?: 1.0
        val height = size?.getOrNull(1) ?: width

        return Hitbox(coords[0], coords[1], coords[2], width, height)
    }

    private fun parseSeat(raw: String): Seat? {
        val parts = raw.trim().split(" ", limit = 2)
        val coords = parts[0].split(",").mapNotNull { it.trim().toDoubleOrNull() }
        if (coords.size != 3) {
            plugin.logger.warning("Furniture $id has invalid seat '$raw' (use \"x,y,z\" with optional yaw)")
            return null
        }

        return Seat(coords[0], coords[1], coords[2], parts.getOrNull(1)?.trim()?.toFloatOrNull())
    }

    private fun parseLight(raw: String): Light? {
        val parts = raw.trim().split(" ", limit = 2)
        val coords = parts[0].split(",").mapNotNull { it.trim().toIntOrNull() }
        val level = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 15
        if (coords.size != 3 || level !in 1..15) {
            plugin.logger.warning("Furniture $id has invalid light '$raw' (use \"x,y,z level\")")
            return null
        }

        return Light(coords[0], coords[1], coords[2], level)
    }
}

/**
 * One furniture state: an alternative look for the display. An empty section
 * keeps the item's own look; a texture/model generates a pack asset named
 * <furniture id>_state_<name>.
 */
class FurnitureState(furnitureId: String, val name: String, val config: Config) {
    val hasAssets = config.has("texture") || config.has("model")

    /** The item_model the display shows in this state; null = the item's own. */
    val modelKey: String? = if (hasAssets) "ecoitems:${furnitureId}_state_$name" else null

    /** Ticks until the furniture advances to the next state by itself. */
    val nextStateAfter = config.getIntOrNull("next-state-after")

    /** Ticks until the furniture returns to its default state. */
    val resetAfter = config.getIntOrNull("reset-after")
}

class FurnitureVehicle(config: Config) {
    /** Horizontal blocks per tick at full throttle. */
    val speed = config.getDoubleOrNull("speed") ?: 0.3

    /** Vertical blocks per tick while holding jump; 0 = a ground vehicle. */
    val flySpeed = config.getDoubleOrNull("fly-speed") ?: 0.0

    /** Item lookups consumed as fuel; empty = no fuel needed. */
    val fuelItems = config.getStrings("fuel.items")

    /** How long one fuel item lasts. */
    val fuelSeconds = config.getIntOrNull("fuel.per-item-seconds") ?: 120

    val needsFuel: Boolean
        get() = fuelItems.isNotEmpty()
}

enum class StorageType {
    /** One shared inventory per placement, contents drop on break. */
    STORAGE,

    /** A separate inventory per player; contents are lost on break. */
    PERSONAL,

    /** A trash can: contents are discarded when closed. */
    DISPOSAL;

    companion object {
        fun fromID(id: String): StorageType? =
            entries.firstOrNull { it.name.equals(id, ignoreCase = true) }
    }
}

class FurnitureStorage(furnitureId: String, config: Config) {
    val rows = (config.getIntOrNull("rows") ?: 3).coerceIn(1, 6)

    val title: String = config.getStringOrNull("title") ?: "Storage"

    val type = StorageType.fromID(config.getString("type").ifEmpty { "storage" })
        ?: StorageType.STORAGE.also {
            plugin.logger.warning(
                "Furniture $furnitureId has unknown storage type " +
                    "'${config.getString("type")}' (storage, personal, or disposal)"
            )
        }

    val openSound = config.getStringOrNull("open-sound") ?: "minecraft:block.chest.open"
    val closeSound = config.getStringOrNull("close-sound") ?: "minecraft:block.chest.close"
}

/**
 * Row-connecting looks: pieces re-model themselves by which sides have a
 * matching neighbor (same furniture, same facing). Missing keys keep the
 * item's own look for that shape. Corner keys connect rows meeting at 90
 * degrees, counter-style.
 */
class FurnitureConnectable(furnitureId: String, config: Config) {
    /** This piece is the left end of a row (neighbor only on its right). */
    val left = if (config.has("left")) FurnitureState(furnitureId, "left", config.getSubsection("left")) else null

    /** This piece is the right end of a row (neighbor only on its left). */
    val right = if (config.has("right")) FurnitureState(furnitureId, "right", config.getSubsection("right")) else null

    /** Neighbors on both sides. */
    val middle = if (config.has("middle")) FurnitureState(furnitureId, "middle", config.getSubsection("middle")) else null

    /** Two rows meeting at this piece, bending toward its front. */
    val inner = if (config.has("inner")) FurnitureState(furnitureId, "inner", config.getSubsection("inner")) else null

    /** Two rows meeting at this piece, bending away from its front. */
    val outer = if (config.has("outer")) FurnitureState(furnitureId, "outer", config.getSubsection("outer")) else null

    val all: List<FurnitureState>
        get() = listOfNotNull(left, right, middle, inner, outer)
}

class FurnitureDoor(furnitureId: String, config: Config) {
    /** The look while open; null keeps the closed look. */
    val openState = if (config.has("open")) {
        FurnitureState(furnitureId, "open", config.getSubsection("open"))
    } else {
        null
    }

    val openSound = config.getStringOrNull("open-sound") ?: "minecraft:block.wooden_door.open"
    val closeSound = config.getStringOrNull("close-sound") ?: "minecraft:block.wooden_door.close"
}
