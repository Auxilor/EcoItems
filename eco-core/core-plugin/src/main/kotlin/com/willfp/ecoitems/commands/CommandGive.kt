package com.willfp.ecoitems.commands

import com.willfp.ecoitems.ext.withMaterialArgs
import com.willfp.ecoitems.items.EcoItems
import com.willfp.ecoitems.util.runFor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import wiki.ecohub.core.command.CommandBuilder
import wiki.ecohub.game.drop.DropQueue

private const val AMOUNT_ARGUMENT_COUNT = 3

private val numbers = listOf(
    "1",
    "2",
    "3",
    "4",
    "5",
    "10",
    "32",
    "64"
)

internal fun CommandBuilder<CommandSender>.giveCommand() = subcommand(
    name = "give",
    permission = "ecoitems.command.give"
) {
    executes {
        val player = argument(0, missingKey = "invalid-player", invalidKey = "invalid-player") { Bukkit.getPlayer(it) }

        val ecoItem = EcoItems.getByID(args.getOrNull(1)) ?: fail("invalid-item")

        val amountArg = args.getOrNull(2)?.toIntOrNull()
        val amount = amountArg?.takeIf { it > 0 } ?: 1

        val message = plugin.langYml.getMessage("give-success")
            .replace(oldValue = "%item%", newValue = ecoItem.id.key)
            .replace(oldValue = "%recipient%", newValue = player.name)

        sender.sendMessage(message)

        val item = ecoItem.itemStack.withMaterialArgs(args.drop(if (amountArg == null) 2 else 3))

        item.amount = amount

        plugin.runFor(player) {
            DropQueue(player)
                .addItem(item)
                .forceTelekinesis()
                .push()
        }
    }

    completes {
        when (args.size) {
            1 -> playerNames()
            2 -> EcoItems.values().filterNot { it.excludeFromCommands }.map { it.id.key }
            AMOUNT_ARGUMENT_COUNT -> numbers
            else -> emptyList()
        }
    }
}
