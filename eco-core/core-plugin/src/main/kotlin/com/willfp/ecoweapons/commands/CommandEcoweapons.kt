package com.willfp.ecoweapons.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.CommandHandler
import com.willfp.eco.core.command.impl.PluginCommand

class CommandEcoweapons(
    plugin: EcoPlugin
) : PluginCommand(
        plugin,
        "ecoweapons",
        "ecoweapons.command.ecoweapons",
        false) {
    init {
        addSubcommand(CommandReload(plugin))
            .addSubcommand(CommandGive(plugin))
    }
    override fun getHandler(): CommandHandler {
        return CommandHandler { sender, _ ->
            sender.sendMessage(
                plugin.langYml.getMessage("invalid-command")
            )
        }
    }
}
