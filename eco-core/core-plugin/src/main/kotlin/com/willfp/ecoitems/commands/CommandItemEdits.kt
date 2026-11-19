package com.willfp.ecoitems.commands

import org.bukkit.command.CommandSender
import org.bukkit.inventory.meta.Damageable
import wiki.ecohub.core.command.CommandBuilder
import wiki.ecohub.ui.text.formatEco

/**
 * Held-item editing: rename (glyph/color aware), repair, and set durability.
 * They work on any item, custom or vanilla, like their IA counterparts.
 */
@Suppress("DEPRECATION")
internal fun CommandBuilder<CommandSender>.renameCommand() = playerSubcommand(
    name = "rename",
    permission = "ecoitems.command.rename"
) {
    executes {
        val stack = sender.inventory.itemInMainHand

        if (stack.type.isAir) {
            fail("must-hold-item")
        }

        if (args.isEmpty()) {
            fail("rename-usage")
        }

        val name = args.joinToString(" ").formatEco(player = sender, formatPlaceholders = true)
        val meta = stack.itemMeta
        if (meta != null) {
            meta.setDisplayName(name)
            stack.itemMeta = meta

            sender.sendMessage(plugin.langYml.getMessage("renamed").replace(oldValue = "%name%", newValue = name))
        }
    }
}

internal fun CommandBuilder<CommandSender>.repairCommand() = playerSubcommand(
    name = "repair",
    permission = "ecoitems.command.repair"
) {
    executes {
        val stack = sender.inventory.itemInMainHand
        val meta = stack.itemMeta as? Damageable

        if (stack.type.isAir || meta == null) {
            fail("not-damageable")
        }

        meta.damage = 0
        stack.itemMeta = meta
        reply("repaired")
    }
}

internal fun CommandBuilder<CommandSender>.durabilityCommand() = playerSubcommand(
    name = "durability",
    permission = "ecoitems.command.durability"
) {
    executes {
        val stack = sender.inventory.itemInMainHand
        val meta = stack.itemMeta as? Damageable

        if (stack.type.isAir || meta == null) {
            fail("not-damageable")
        }

        val value = args.getOrNull(0)?.toIntOrNull() ?: fail("durability-usage")

        val max = if (meta.hasMaxDamage()) meta.maxDamage else stack.type.maxDurability.toInt()
        meta.damage = (max - value).coerceIn(minimumValue = 0, maximumValue = max)
        stack.itemMeta = meta

        sender.sendMessage(
            plugin.langYml.getMessage("durability-set")
                .replace(oldValue = "%durability%", newValue = (max - meta.damage).toString())
                .replace(oldValue = "%max%", newValue = max.toString())
        )
    }
}
