package com.willfp.ecoitems.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecoitems.items.EcoItems
import com.willfp.ecoitems.items.ecoItem
import com.willfp.ecoitems.plugin
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.util.StringUtil

object CommandTake : Subcommand(plugin, "take", "ecoitems.command.take", false) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        val player = notifyPlayerRequired(args.getOrNull(0), "invalid-player")
        val ecoItem = notifyNull(EcoItems.getByID(args.getOrNull(1)), "invalid-item")

        var remaining = args.getOrNull(2)?.toIntOrNull() ?: Int.MAX_VALUE
        var taken = 0

        val inventory = player.inventory
        for (slot in 0 until inventory.size) {
            if (remaining <= 0) {
                break
            }

            val stack = inventory.getItem(slot) ?: continue
            if (stack.ecoItem != ecoItem) {
                continue
            }

            val take = minOf(stack.amount, remaining)
            taken += take
            remaining -= take

            if (take >= stack.amount) {
                inventory.setItem(slot, null)
            } else {
                stack.amount -= take
            }
        }

        sender.sendMessage(
            plugin.langYml.getMessage("took-items")
                .replace("%amount%", taken.toString())
                .replace("%item%", ecoItem.id.key)
                .replace("%recipient%", player.name)
        )
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        val completions = mutableListOf<String>()

        if (args.size == 1) {
            StringUtil.copyPartialMatches(args[0], Bukkit.getOnlinePlayers().map { it.name }, completions)
            return completions
        }

        if (args.size == 2) {
            StringUtil.copyPartialMatches(
                args[1],
                EcoItems.values().filterNot { it.excludeFromCommands }.map { it.id.key },
                completions
            )
            completions.sort()
            return completions
        }

        return emptyList()
    }
}
