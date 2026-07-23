package com.willfp.ecoitems.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecoitems.dialogs.Dialogs
import com.willfp.ecoitems.plugin
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.util.StringUtil

object CommandDialog : Subcommand(plugin, "dialog", "ecoitems.command.dialog", false) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        val dialog = notifyNull(Dialogs.getByID(args.getOrNull(0)), "invalid-dialog")

        val player = args.getOrNull(1)?.let { notifyPlayerRequired(it, "invalid-player") }
            ?: (sender as? Player ?: run {
                sender.sendMessage(plugin.langYml.getMessage("invalid-player"))
                return
            })

        if (!dialog.open(player)) {
            sender.sendMessage(plugin.langYml.getMessage("dialogs-unsupported"))
        }
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        val completions = mutableListOf<String>()

        if (args.size == 1) {
            StringUtil.copyPartialMatches(args[0], Dialogs.values().map { it.id }, completions)
            return completions
        }

        if (args.size == 2) {
            StringUtil.copyPartialMatches(args[1], Bukkit.getOnlinePlayers().map { it.name }, completions)
            return completions
        }

        return emptyList()
    }
}
