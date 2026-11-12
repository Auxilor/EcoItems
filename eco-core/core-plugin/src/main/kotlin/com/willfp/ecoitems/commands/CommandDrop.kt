package com.willfp.ecoitems.commands

import com.willfp.ecoitems.items.EcoItem
import com.willfp.ecoitems.items.EcoItems
import com.willfp.ecoitems.util.runAt
import com.willfp.ecoitems.util.runFor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.inventory.ItemStack
import wiki.ecohub.core.command.CommandBuilder

private const val PLAYER_AMOUNT_INDEX = 2
private const val COORDINATE_COUNT = 3
private const val WORLD_INDEX = 4
private const val LOCATION_AMOUNT_INDEX = 5

/**
 * Drops an item at a player, or at a location given as "x y z world". A player's location is read on the
 * player's own thread.
 */
internal fun CommandBuilder<CommandSender>.dropCommand() = subcommand(
    name = "drop",
    permission = "ecoitems.command.drop"
) {
    executes {
        val ecoItem = EcoItems.getByID(args.getOrNull(0)) ?: fail("invalid-item")

        val player = args.getOrNull(1)?.let { Bukkit.getPlayer(it) }
        if (player != null) {
            val stack = ecoItem.dropStack(args.getOrNull(PLAYER_AMOUNT_INDEX))
            plugin.runFor(player) { player.world.dropItemNaturally(player.location, stack) }
        } else {
            val target = locationOf(args) ?: fail("invalid-location")
            val stack = ecoItem.dropStack(args.getOrNull(LOCATION_AMOUNT_INDEX))
            plugin.runAt(target) { target.world.dropItemNaturally(target, stack) }
        }

        sender.sendMessage(
            plugin.langYml.getMessage("dropped-item")
                .replace(oldValue = "%item%", newValue = ecoItem.id.key)
        )
    }

    completes {
        when (args.size) {
            1 -> EcoItems.values().filterNot { it.excludeFromCommands }.map { it.id.key }
            2 -> playerNames()
            else -> emptyList()
        }
    }
}

private fun locationOf(args: List<String>): Location? {
    val coordinates = args.asSequence()
        .drop(1)
        .take(COORDINATE_COUNT)
        .mapNotNull { it.toDoubleOrNull() }
        .toList()
    val world = args.getOrNull(WORLD_INDEX)?.let { Bukkit.getWorld(it) }

    return if (coordinates.size == COORDINATE_COUNT && world != null) {
        Location(world, coordinates[0], coordinates[1], coordinates[2])
    } else {
        null
    }
}

private fun EcoItem.dropStack(amountArgument: String?): ItemStack =
    itemStack.apply {
        amount = (amountArgument?.toIntOrNull() ?: 1).coerceIn(minimumValue = 1, maximumValue = maxStackSize)
    }
