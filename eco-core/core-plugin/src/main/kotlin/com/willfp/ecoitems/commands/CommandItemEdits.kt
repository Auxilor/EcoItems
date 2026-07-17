package com.willfp.ecoitems.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.util.formatEco
import com.willfp.ecoitems.plugin
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.Damageable

/**
 * Held-item editing: rename (glyph/color aware), repair, and set durability.
 * They work on any item, custom or vanilla, like their IA counterparts.
 */
object CommandRename : Subcommand(plugin, "rename", "ecoitems.command.rename", true) {
    @Suppress("DEPRECATION")
    override fun onExecute(sender: CommandSender, args: List<String>) {
        val player = sender as Player
        val stack = player.inventory.itemInMainHand

        if (stack.type.isAir) {
            sender.sendMessage(plugin.langYml.getMessage("must-hold-item"))
            return
        }

        if (args.isEmpty()) {
            sender.sendMessage(plugin.langYml.getMessage("rename-usage"))
            return
        }

        val name = args.joinToString(" ").formatEco(player = player, formatPlaceholders = true)
        val meta = stack.itemMeta ?: return
        meta.setDisplayName(name)
        stack.itemMeta = meta

        sender.sendMessage(plugin.langYml.getMessage("renamed").replace("%name%", name))
    }
}

object CommandRepair : Subcommand(plugin, "repair", "ecoitems.command.repair", true) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        val player = sender as Player
        val stack = player.inventory.itemInMainHand
        val meta = stack.itemMeta as? Damageable

        if (stack.type.isAir || meta == null) {
            sender.sendMessage(plugin.langYml.getMessage("not-damageable"))
            return
        }

        meta.damage = 0
        stack.itemMeta = meta
        sender.sendMessage(plugin.langYml.getMessage("repaired"))
    }
}

object CommandDurability : Subcommand(plugin, "durability", "ecoitems.command.durability", true) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        val player = sender as Player
        val stack = player.inventory.itemInMainHand
        val meta = stack.itemMeta as? Damageable

        if (stack.type.isAir || meta == null) {
            sender.sendMessage(plugin.langYml.getMessage("not-damageable"))
            return
        }

        val value = args.getOrNull(0)?.toIntOrNull() ?: run {
            sender.sendMessage(plugin.langYml.getMessage("durability-usage"))
            return
        }

        val max = if (meta.hasMaxDamage()) meta.maxDamage else stack.type.maxDurability.toInt()
        meta.damage = (max - value).coerceIn(0, max)
        stack.itemMeta = meta

        sender.sendMessage(
            plugin.langYml.getMessage("durability-set")
                .replace("%durability%", (max - meta.damage).toString())
                .replace("%max%", max.toString())
        )
    }
}
