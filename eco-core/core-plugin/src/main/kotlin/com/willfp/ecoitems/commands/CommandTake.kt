package com.willfp.ecoitems.commands

import com.willfp.ecoitems.items.EcoItem
import com.willfp.ecoitems.items.EcoItems
import com.willfp.ecoitems.items.ecoItem
import com.willfp.ecoitems.util.runFor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import wiki.ecohub.core.command.CommandBuilder

/**
 * Takes an item from a player. The inventory belongs to the player's thread, which on Folia need
 * not be this one.
 */
internal fun CommandBuilder<CommandSender>.takeCommand() = subcommand(
    name = "take",
    permission = "ecoitems.command.take"
) {
    executes {
        val player = argument(0, missingKey = "invalid-player", invalidKey = "invalid-player") { Bukkit.getPlayer(it) }
        val ecoItem = EcoItems.getByID(args.getOrNull(1)) ?: fail("invalid-item")

        val limit = args.getOrNull(2)?.toIntOrNull() ?: Int.MAX_VALUE

        plugin.runFor(player) {
            val taken = take(player, ecoItem, limit)

            sender.sendMessage(
                plugin.langYml.getMessage("took-items")
                    .replace(oldValue = "%amount%", newValue = taken.toString())
                    .replace(oldValue = "%item%", newValue = ecoItem.id.key)
                    .replace(oldValue = "%recipient%", newValue = player.name)
            )
        }
    }

    completes {
        when (args.size) {
            1 -> playerNames()
            2 -> EcoItems.values().filterNot { it.excludeFromCommands }.map { it.id.key }
            else -> emptyList()
        }
    }
}

/** Removes up to [limit] of [ecoItem] from the player; how many were taken. */
internal fun take(player: Player, ecoItem: EcoItem, limit: Int): Int {
    var remaining = limit
    var taken = 0

    val inventory = player.inventory
    for (slot in 0 until inventory.size) {
        val stack = inventory.getItem(slot)
        if (remaining > 0 && stack != null && stack.ecoItem == ecoItem) {
            val take = minOf(a = stack.amount, b = remaining)
            taken += take
            remaining -= take

            if (take >= stack.amount) {
                inventory.setItem(slot, null)
            } else {
                stack.amount -= take
            }
        }
    }

    return taken
}
