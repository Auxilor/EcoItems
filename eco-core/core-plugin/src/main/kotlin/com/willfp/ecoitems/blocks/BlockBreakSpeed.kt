package com.willfp.ecoitems.blocks

import com.willfp.ecoitems.plugin
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageAbortEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack

/**
 * Custom hardness. The backing block has a fixed client-side break time, so
 * a transient block_break_speed attribute modifier rescales the player's
 * mining speed while they dig - the attribute syncs to the client, keeping
 * the crack animation honest.
 *
 * String blocks are insta-mine in vanilla, which no attribute can slow down,
 * so hardness only applies to noteblock and chorus backings.
 */
object BlockBreakSpeed : Listener {
    private val key = NamespacedKey(plugin, "block_hardness")

    @EventHandler(ignoreCancelled = true)
    fun onDamage(event: BlockDamageEvent) {
        val placed = EcoBlocks.at(event.block) ?: return
        val block = placed.block

        if (block.hardness < 0 || block.backing == BlockBacking.STRINGBLOCK) {
            return
        }

        if (block.hardness == 0.0) {
            event.instaBreak = true
            return
        }

        val tool = event.player.inventory.itemInMainHand
        val backingHardness = when (block.backing) {
            BlockBacking.NOTEBLOCK -> 0.8
            BlockBacking.CHORUS -> 0.4
            BlockBacking.STRINGBLOCK -> return
        }

        // What vanilla thinks: axes speed up both backings.
        val actualSpeed = if (tool.type.name.endsWith("_AXE")) toolSpeed(tool) else 1.0
        // What the config wants: correct tools mine at their speed, others at hand speed.
        val correct = block.correctTools.isEmpty() ||
            block.correctTools.any { tool.type.name.endsWith("_$it") }
        val desiredSpeed = if (correct && tool.type.name.contains("_")) toolSpeed(tool) else 1.0

        val factor = (backingHardness / block.hardness) * (desiredSpeed / actualSpeed)

        val attribute = event.player.getAttribute(Attribute.BLOCK_BREAK_SPEED) ?: return
        remove(event.player)
        attribute.addTransientModifier(
            AttributeModifier(key, factor - 1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1)
        )
    }

    @EventHandler
    fun onAbort(event: BlockDamageAbortEvent) = remove(event.player)

    @EventHandler(priority = EventPriority.MONITOR)
    fun onBreak(event: BlockBreakEvent) = remove(event.player)

    @EventHandler
    fun onHeldChange(event: PlayerItemHeldEvent) = remove(event.player)

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) = remove(event.player)

    private fun remove(player: Player) {
        val attribute = player.getAttribute(Attribute.BLOCK_BREAK_SPEED) ?: return
        attribute.modifiers.filter { it.key == key }.forEach { attribute.removeModifier(it) }
    }

    private fun toolSpeed(tool: ItemStack): Double {
        val base = when (tool.type.name.substringBefore("_")) {
            "WOODEN" -> 2.0
            "STONE" -> 4.0
            "IRON" -> 6.0
            "DIAMOND" -> 8.0
            "NETHERITE" -> 9.0
            "GOLDEN" -> 12.0
            else -> 1.0
        }

        val efficiency = tool.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.EFFICIENCY)
        return base + if (efficiency > 0) efficiency * efficiency + 1.0 else 0.0
    }
}
