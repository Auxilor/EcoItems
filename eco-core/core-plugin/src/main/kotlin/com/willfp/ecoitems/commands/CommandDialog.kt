package com.willfp.ecoitems.commands

import com.willfp.ecoitems.dialogs.Dialogs
import com.willfp.ecoitems.util.runFor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import wiki.ecohub.core.command.CommandBuilder

internal fun CommandBuilder<CommandSender>.dialogCommand() = subcommand(
    name = "dialog",
    permission = "ecoitems.command.dialog"
) {
    executes {
        val dialog = Dialogs.getByID(args.getOrNull(0)) ?: fail("invalid-dialog")

        val player = if (args.size > 1) {
            argument(1, missingKey = "invalid-player", invalidKey = "invalid-player") { Bukkit.getPlayer(it) }
        } else {
            sender as? Player ?: fail("invalid-player")
        }

        plugin.runFor(player) {
            if (!dialog.open(player)) {
                reply("dialogs-unsupported")
            }
        }
    }

    completes {
        when (args.size) {
            1 -> Dialogs.values().map { it.id }
            2 -> playerNames()
            else -> emptyList()
        }
    }
}
