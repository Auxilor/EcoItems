package com.willfp.ecoitems.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecoitems.furniture.PlacedFurniture
import com.willfp.ecoitems.plugin
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.command.CommandSender
import org.bukkit.entity.Interaction
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.util.BoundingBox

/**
 * Outlines nearby furniture for a few seconds: barrier cells in red,
 * interaction hitboxes in yellow.
 */
object CommandHitbox : Subcommand(plugin, "hitbox", "ecoitems.command.hitbox", true) {
    private const val RADIUS = 10.0
    private const val REPEATS = 12
    private const val PERIOD_TICKS = 10L
    private const val STEP = 0.25

    override fun onExecute(sender: CommandSender, args: List<String>) {
        val player = sender as Player

        val furniture = player.getNearbyEntities(RADIUS, RADIUS, RADIUS)
            .filterIsInstance<ItemDisplay>()
            .mapNotNull { PlacedFurniture.fromEntity(it) }

        if (furniture.isEmpty()) {
            sender.sendMessage(plugin.langYml.getMessage("no-furniture-nearby"))
            return
        }

        val barriers = furniture.flatMap { it.barrierBlocks() }
            .filter { it.type == Material.BARRIER }
            .map { BoundingBox.of(it) }
        val hitboxes = furniture.flatMap { it.interactionEntities() }
            .filterIsInstance<Interaction>()
            .map { it.boundingBox }

        var remaining = REPEATS
        lateinit var draw: () -> Unit
        draw = {
            if (player.isOnline && remaining-- > 0) {
                barriers.forEach { outline(player, it, Color.RED) }
                hitboxes.forEach { outline(player, it, Color.YELLOW) }
                plugin.scheduler.runLater(PERIOD_TICKS, draw)
            }
        }
        draw()

        sender.sendMessage(plugin.langYml.getMessage("hitbox-shown"))
    }

    private fun outline(player: Player, box: BoundingBox, color: Color) {
        val dust = Particle.DustOptions(color, 0.6f)
        val xs = doubleRange(box.minX, box.maxX)
        val ys = doubleRange(box.minY, box.maxY)
        val zs = doubleRange(box.minZ, box.maxZ)

        // The 12 edges: hold two axes at their extremes, sweep the third.
        for (x in xs) for (y in listOf(box.minY, box.maxY)) for (z in listOf(box.minZ, box.maxZ)) {
            player.spawnParticle(Particle.DUST, x, y, z, 1, dust)
        }
        for (y in ys) for (x in listOf(box.minX, box.maxX)) for (z in listOf(box.minZ, box.maxZ)) {
            player.spawnParticle(Particle.DUST, x, y, z, 1, dust)
        }
        for (z in zs) for (x in listOf(box.minX, box.maxX)) for (y in listOf(box.minY, box.maxY)) {
            player.spawnParticle(Particle.DUST, x, y, z, 1, dust)
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
}
