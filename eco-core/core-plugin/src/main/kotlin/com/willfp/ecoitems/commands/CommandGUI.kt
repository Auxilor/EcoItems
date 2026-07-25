package com.willfp.ecoitems.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecoitems.items.ItemsGUI
import com.willfp.ecoitems.plugin
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object CommandGUI : Subcommand(plugin, "gui", "ecoitems.command.gui", true) {
    override fun onExecute(player: CommandSender, args: List<String>) {
        ItemsGUI.open(player as Player)
    }
}
