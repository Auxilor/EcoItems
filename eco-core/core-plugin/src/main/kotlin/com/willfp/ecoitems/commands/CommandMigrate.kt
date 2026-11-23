package com.willfp.ecoitems.commands

import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.migration.Migrations
import org.bukkit.command.CommandSender
import wiki.ecohub.core.command.CommandBuilder

internal fun CommandBuilder<CommandSender>.migrateCommand() = subcommand(
    name = "migrate",
    permission = "ecoitems.command.migrate"
) {
    executes {
        val source = args.firstOrNull() ?: fail("migrate-usage")

        val result = Migrations.migrate(plugin as EcoItemsPlugin, source) ?: fail("migrate-usage")

        sender.sendMessage(
            plugin.langYml.getMessage("migrated")
                .replace(oldValue = "%summary%", newValue = result.summary())
        )

        plugin.reload()
    }

    completes {
        if (args.size == 1) Migrations.SOURCES.map { it.lowercase() } else emptyList()
    }
}
