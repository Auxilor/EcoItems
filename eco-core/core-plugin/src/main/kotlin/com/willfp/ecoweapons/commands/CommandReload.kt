package com.willfp.ecoweapons.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.core.command.CommandHandler
import org.bukkit.command.CommandSender

class CommandReload(plugin: EcoPlugin) : Subcommand(plugin, "reload", "ecoweapons.command.reload", false) {
    override fun getHandler(): CommandHandler {
        return CommandHandler { sender: CommandSender, _: List<String?>? ->
            plugin.reload()
            sender.sendMessage(plugin.langYml.getMessage("reloaded"))
        }
    }
}