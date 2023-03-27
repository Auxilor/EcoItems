package com.willfp.ecoitems.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.core.drops.DropQueue
import com.willfp.ecoitems.items.EcoItems
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.util.StringUtil

class CommandGive(plugin: EcoPlugin) : Subcommand(plugin, "give", "ecoitems.command.give", false) {
    private val numbers = listOf(
        "1",
        "2",
        "3",
        "4",
        "5",
        "10",
        "32",
        "64"
    )

    override fun onExecute(sender: CommandSender, args: List<String>) {
        if (args.isEmpty()) {
            sender.sendMessage(plugin.langYml.getMessage("needs-player"))
            return
        }
        if (args.size == 1) {
            sender.sendMessage(plugin.langYml.getMessage("needs-item"))
            return
        }
        val receiverName = args[0]
        val receiver = Bukkit.getPlayer(receiverName)
        if (receiver == null) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-player"))
            return
        }
        val itemID = args[1]
        var amount = 1

        val ecoItem = EcoItems.getByID(itemID.lowercase())
        if (ecoItem == null) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-item"))
            return
        }

        var message = plugin.langYml.getMessage("give-success")
        message = message.replace("%item%", itemID).replace("%recipient%", receiver.name)

        sender.sendMessage(message)

        if (args.size == 3) {
            amount = args[2].toIntOrNull() ?: 1
        }

        val item = ecoItem.itemStack

        item.amount = amount

        DropQueue(receiver)
            .addItem(item)
            .forceTelekinesis()
            .push()
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        val completions = mutableListOf<String>()

        if (args.isEmpty()) {
            return EcoItems.values().map { it.id.key }
        }

        if (args.size == 1) {
            StringUtil.copyPartialMatches(
                args[0],
                Bukkit.getOnlinePlayers().map { it.name },
                completions
            )
            return completions
        }

        if (args.size == 2) {
            val itemNames = EcoItems.values().map { it.id.key }

            StringUtil.copyPartialMatches(args[1], itemNames, completions)
            completions.sort()
            return completions
        }

        if (args.size == 3) {
            StringUtil.copyPartialMatches(args[2], numbers, completions)
            return completions
        }

        return emptyList()
    }
}
