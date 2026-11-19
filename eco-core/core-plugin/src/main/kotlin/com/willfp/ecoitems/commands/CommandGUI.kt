package com.willfp.ecoitems.commands

import com.willfp.ecoitems.items.ItemsGUI
import org.bukkit.command.CommandSender
import wiki.ecohub.core.command.CommandBuilder

internal fun CommandBuilder<CommandSender>.guiCommand() = playerSubcommand(
    name = "gui",
    permission = "ecoitems.command.gui"
) {
    executes {
        ItemsGUI.open(sender)
    }
}
