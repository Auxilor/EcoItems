package com.willfp.ecoitems.commands

import com.willfp.ecoitems.items.EcoItem
import com.willfp.ecoitems.items.EcoItems
import com.willfp.ecoitems.plugin
import com.willfp.ecoitems.util.runFor
import org.bukkit.Bukkit
import org.bukkit.EntityEffect
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import wiki.ecohub.core.command.CommandBuilder

internal fun CommandBuilder<CommandSender>.totemCommand() = subcommand(
    name = "totem",
    permission = "ecoitems.command.totem"
) {
    executes {
        val ecoItem = EcoItems.getByID(args.getOrNull(0)) ?: fail("invalid-item")

        val player = if (args.size > 1) {
            argument(1, missingKey = "invalid-player", invalidKey = "invalid-player") { Bukkit.getPlayer(it) }
        } else {
            sender as? Player ?: fail("invalid-player")
        }

        plugin.runFor(player) { playTotemAnimation(player, ecoItem) }

        sender.sendMessage(
            plugin.langYml.getMessage("totem-played")
                .replace(oldValue = "%item%", newValue = ecoItem.id.key)
                .replace(oldValue = "%recipient%", newValue = player.name)
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

/**
 * The client renders the totem pop with whatever sits in the offhand, so
 * the item goes there for a tick around the effect and is put back after.
 */
fun playTotemAnimation(player: Player, item: EcoItem) {
    val previous = player.inventory.itemInOffHand

    player.inventory.setItemInOffHand(item.itemStack)
    player.playEffect(EntityEffect.TOTEM_RESURRECT)

    plugin.scheduler.on(player).run {
        if (player.isOnline) {
            player.inventory.setItemInOffHand(previous)
        }
    }
}
