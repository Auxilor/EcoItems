package com.willfp.ecoitems.commands

import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.ecoitems.items.EcoItems
import com.willfp.libreforge.LibReforgePlugin
import com.willfp.libreforge.lrcdb.CommandExport
import com.willfp.libreforge.lrcdb.CommandImport
import com.willfp.libreforge.lrcdb.ExportableConfig
import org.bukkit.command.CommandSender

class CommandEcoItems(
    plugin: LibReforgePlugin
) : PluginCommand(
        plugin,
        "ecoitems",
        "ecoitems.command.ecoitems",
        false) {
    init {
        addSubcommand(CommandReload(plugin))
            .addSubcommand(CommandGive(plugin))
            .addSubcommand(CommandImport("items", plugin))
            .addSubcommand(CommandExport(plugin) {
                EcoItems.values().map {
                    ExportableConfig(
                        it.id,
                        it.config
                    )
                }
            })
    }

    override fun onExecute(sender: CommandSender, args: List<String>) {
        sender.sendMessage(
            plugin.langYml.getMessage("invalid-command")
        )
    }
}
