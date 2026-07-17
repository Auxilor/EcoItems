package com.willfp.ecoitems.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecoitems.items.EcoItem
import com.willfp.ecoitems.items.EcoItems
import com.willfp.ecoitems.plugin
import org.bukkit.Bukkit
import org.bukkit.EntityEffect
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.util.StringUtil

object CommandTotem : Subcommand(plugin, "totem", "ecoitems.command.totem", false) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        val ecoItem = notifyNull(EcoItems.getByID(args.getOrNull(0)), "invalid-item")

        val player = args.getOrNull(1)?.let { notifyPlayerRequired(it, "invalid-player") }
            ?: (sender as? Player ?: run {
                sender.sendMessage(plugin.langYml.getMessage("invalid-player"))
                return
            })

        playTotemAnimation(player, ecoItem)

        sender.sendMessage(
            plugin.langYml.getMessage("totem-played")
                .replace("%item%", ecoItem.id.key)
                .replace("%recipient%", player.name)
        )
    }

    /**
     * The client renders the totem pop with whatever sits in the offhand, so
     * the item goes there for a tick around the effect and is put back after.
     */
    fun playTotemAnimation(player: Player, item: EcoItem) {
        val previous = player.inventory.itemInOffHand

        player.inventory.setItemInOffHand(item.itemStack)
        player.playEffect(EntityEffect.TOTEM_RESURRECT)

        plugin.scheduler.run {
            if (player.isOnline) {
                player.inventory.setItemInOffHand(previous)
            }
        }
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        val completions = mutableListOf<String>()

        if (args.size == 1) {
            StringUtil.copyPartialMatches(args[0], EcoItems.values().map { it.id.key }, completions)
            completions.sort()
            return completions
        }

        if (args.size == 2) {
            StringUtil.copyPartialMatches(args[1], Bukkit.getOnlinePlayers().map { it.name }, completions)
            return completions
        }

        return emptyList()
    }
}
