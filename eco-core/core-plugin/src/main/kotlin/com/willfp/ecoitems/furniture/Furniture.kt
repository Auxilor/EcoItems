package com.willfp.ecoitems.furniture

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecoitems.blocks.BlockDrops
import com.willfp.ecoitems.blocks.BlockSounds
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

    /** Solid collision cells, relative to the origin block. */
    val barriers: List<Cell> = config.getStrings("barriers").flatMap { parseCells(it) }

    /**
     * Click hitboxes: "x,y,z" or "x,y,z widthxheight". Defaults to a single
     * 1x1 box at the origin when there are no barriers to click instead.
     */
    val hitboxes: List<Hitbox> = config.getStrings("hitboxes").mapNotNull { parseHitbox(it) }
        .ifEmpty { if (barriers.isEmpty()) listOf(Hitbox(0.0, 0.0, 0.0, 1.0, 1.0)) else emptyList() }

    /** Seat offsets: "x,y,z" with an optional yaw ("x,y,z 90"). */
    val seats: List<Seat> = config.getStrings("seats").mapNotNull { parseSeat(it) }

    /** Light cells: "x,y,z level". */
    val lights: List<Light> = config.getStrings("lights").mapNotNull { parseLight(it) }

    /** Right-clicking toggles the lights on and off. */
    val toggleableLights = config.getBool("toggleable-lights")

    /** null = drop the placer item itself. */
    val drops = if (config.has("drops")) BlockDrops(id, config.getSubsection("drops")) else null

    val sounds = if (config.has("sounds")) BlockSounds(config.getSubsection("sounds")) else null

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
