package com.willfp.ecoitems.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecoitems.migration.Migrations
import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.plugin
import org.bukkit.command.CommandSender
import org.bukkit.util.StringUtil

object CommandMigrate : Subcommand(plugin, "migrate", "ecoitems.command.migrate", false) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        val source = args.firstOrNull()
        if (source == null) {
            sender.sendMessage(plugin.langYml.getMessage("migrate-usage"))
            return
        }

        val result = Migrations.migrate(plugin as EcoItemsPlugin, source)
        if (result == null) {
            sender.sendMessage(plugin.langYml.getMessage("migrate-usage"))
            return
        }

        sender.sendMessage(
            plugin.langYml.getMessage("migrated")
                .replace("%summary%", result.summary())
        )

        plugin.reload()
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        if (args.size == 1) {
            return StringUtil.copyPartialMatches(
                args[0],
                Migrations.SOURCES.map { it.lowercase() },
                mutableListOf()
            )
        }
        return emptyList()
    }
}
