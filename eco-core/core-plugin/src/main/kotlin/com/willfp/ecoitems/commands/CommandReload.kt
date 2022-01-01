package com.willfp.ecoitems.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.core.command.CommandHandler
import org.bukkit.command.CommandSender

class CommandReload(plugin: EcoPlugin) : Subcommand(plugin, "reload", "ecoitems.command.reload", false) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        plugin.reload()
        sender.sendMessage(plugin.langYml.getMessage("reloaded"))
    }
}