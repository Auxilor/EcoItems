package com.willfp.ecoitems.commands

import com.willfp.ecoitems.furniture.PlacedFurniture
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.command.CommandSender
import org.bukkit.entity.Interaction
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import wiki.ecohub.core.command.CommandBuilder

private const val RADIUS = 10.0
private const val REPEATS = 12
private const val PERIOD_TICKS = 10L
private const val STEP = 0.25

/**
 * Outlines nearby furniture for a few seconds: barrier cells in red,
 * interaction hitboxes in yellow.
 */
internal fun CommandBuilder<CommandSender>.hitboxCommand() = playerSubcommand(
    name = "hitbox",
    permission = "ecoitems.command.hitbox"
) {
    executes {
        val furniture = sender.getNearbyEntities(RADIUS, RADIUS, RADIUS)
            .filterIsInstance<ItemDisplay>()
            .mapNotNull { PlacedFurniture.fromEntity(it) }

        if (furniture.isEmpty()) {
            fail("no-furniture-nearby")
        }

        val barriers = furniture.asSequence()
            .flatMap { it.barrierBlocks() }
            .filter { it.type == Material.BARRIER }
            .map { BoundingBox.of(it) }
            .toList()
        val hitboxes = furniture.asSequence()
            .flatMap { it.interactionEntities() }
            .filterIsInstance<Interaction>()
            .map { it.boundingBox }
            .toList()

        var remaining = REPEATS
        lateinit var draw: () -> Unit
        draw = {
            if (sender.isOnline && remaining-- > 0) {
                barriers.forEach { outline(sender, it, Color.RED) }
                hitboxes.forEach { outline(sender, it, Color.YELLOW) }
                plugin.scheduler.on(sender).runLater(PERIOD_TICKS, draw)
            }
        }
        draw()

        reply("hitbox-shown")
    }
}

private fun outline(player: Player, box: BoundingBox, color: Color) {
    val dust = Particle.DustOptions(color, 0.6f)

    for (point in edgePoints(box)) {
        player.spawnParticle(Particle.DUST, point.x, point.y, point.z, 1, dust)
    }
}

/** The 12 edges: hold two axes at their extremes, sweep the third. */
internal fun edgePoints(box: BoundingBox): List<Vector> {
    val xEdges = listOf(box.minX, box.maxX)
    val yEdges = listOf(box.minY, box.maxY)
    val zEdges = listOf(box.minZ, box.maxZ)

    return doubleRange(from = box.minX, to = box.maxX).flatMap { x ->
        yEdges.flatMap { y -> zEdges.map { z -> Vector(x, y, z) } }
    } + doubleRange(from = box.minY, to = box.maxY).flatMap { y ->
        xEdges.flatMap { x -> zEdges.map { z -> Vector(x, y, z) } }
    } + doubleRange(from = box.minZ, to = box.maxZ).flatMap { z ->
        xEdges.flatMap { x -> yEdges.map { y -> Vector(x, y, z) } }
    }
}

private fun doubleRange(from: Double, to: Double): List<Double> {
    val values = mutableListOf<Double>()
    var current = from
    while (current <= to) {
        values += current
        current += STEP
    }
    return values
}
