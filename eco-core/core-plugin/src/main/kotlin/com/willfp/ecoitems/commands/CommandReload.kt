package com.willfp.ecoitems.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecoitems.plugin
import org.bukkit.command.CommandSender

object CommandReload : Subcommand(plugin, "reload", "ecoitems.command.reload", false) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        plugin.reload()
        sender.sendMessage(plugin.langYml.getMessage("reloaded"))
    }
}