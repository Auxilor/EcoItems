package com.willfp.ecoitems.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecoitems.items.EcoItems
import com.willfp.ecoitems.plugin
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.util.StringUtil

object CommandDrop : Subcommand(plugin, "drop", "ecoitems.command.drop", false) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        val ecoItem = notifyNull(EcoItems.getByID(args.getOrNull(0)), "invalid-item")

        // Either a player name or "x y z world".
        val target: Location
        val amountIndex: Int

        val player = args.getOrNull(1)?.let { Bukkit.getPlayer(it) }
        if (player != null) {
            target = player.location
            amountIndex = 2
        } else {
            val x = args.getOrNull(1)?.toDoubleOrNull()
            val y = args.getOrNull(2)?.toDoubleOrNull()
            val z = args.getOrNull(3)?.toDoubleOrNull()
            val world = args.getOrNull(4)?.let { Bukkit.getWorld(it) }

            if (x == null || y == null || z == null || world == null) {
                sender.sendMessage(plugin.langYml.getMessage("invalid-location"))
                return
            }

            target = Location(world, x, y, z)
            amountIndex = 5
        }

        val amount = args.getOrNull(amountIndex)?.toIntOrNull() ?: 1

        val stack = ecoItem.itemStack
        stack.amount = amount.coerceIn(1, stack.maxStackSize)
        target.world!!.dropItemNaturally(target, stack)

        sender.sendMessage(
            plugin.langYml.getMessage("dropped-item")
                .replace("%item%", ecoItem.id.key)
        )
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        val completions = mutableListOf<String>()

        if (args.size == 1) {
            StringUtil.copyPartialMatches(
                args[0],
                EcoItems.values().filterNot { it.excludeFromCommands }.map { it.id.key },
                completions
            )
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
