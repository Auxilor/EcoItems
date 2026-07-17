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
            .addSubcommand(CommandGUI)
            .addSubcommand(CommandHud)
            .addSubcommand(CommandGlyph)
            .addSubcommand(CommandGlyphs)
            .addSubcommand(CommandTotem)
            .addSubcommand(CommandDrop)
            .addSubcommand(CommandTake)
            .addSubcommand(CommandRename)
            .addSubcommand(CommandRepair)
            .addSubcommand(CommandDurability)
            .addSubcommand(CommandHitbox)
            .addSubcommand(CommandMigrate)
    }

    override fun getAliases(): List<String> {
        return listOf("ei", "e", "ecoi", "items")
    }

    override fun onExecute(sender: CommandSender, args: List<String>) {
        sender.sendMessage(
            plugin.langYml.getMessage("invalid-command")
        )
    }
}
