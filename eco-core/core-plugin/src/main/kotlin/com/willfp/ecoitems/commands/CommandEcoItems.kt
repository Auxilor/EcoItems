package com.willfp.ecoitems.commands

import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.ecoitems.plugin
import org.bukkit.command.CommandSender

object CommandEcoItems : PluginCommand(
    plugin,
    "ecoitems",
    "ecoitems.command.ecoitems",
    false
) {
    init {
        this.addSubcommand(CommandReload)
            .addSubcommand(CommandGive)
    }

    override fun onExecute(sender: CommandSender, args: List<String>) {
        sender.sendMessage(
            plugin.langYml.getMessage("invalid-command")
        )
    }
}
