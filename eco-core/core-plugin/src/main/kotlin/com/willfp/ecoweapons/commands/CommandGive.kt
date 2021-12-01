package com.willfp.ecoweapons.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.CommandHandler
import com.willfp.eco.core.command.TabCompleteHandler
import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecoweapons.weapons.Weapons
import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack
import org.bukkit.util.StringUtil

class CommandGive(plugin: EcoPlugin) : Subcommand(plugin, "give", "ecoweapons.command.give", false) {
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

    override fun getHandler(): CommandHandler {
        return CommandHandler { sender, args ->
            if (args.isEmpty()) {
                sender.sendMessage(plugin.langYml.getMessage("needs-player"))
                return@CommandHandler
            }
            if (args.size == 1) {
                sender.sendMessage(plugin.langYml.getMessage("needs-item"))
                return@CommandHandler
            }
            val receiverName = args[0]
            val receiver = Bukkit.getPlayer(receiverName)
            if (receiver == null) {
                sender.sendMessage(plugin.langYml.getMessage("invalid-player"))
                return@CommandHandler
            }
            val itemID = args[1]
            var amount = 1
            val weapon = Weapons.getByID(itemID.lowercase().replace("_fuel", ""))
            if (weapon == null) {
                sender.sendMessage(plugin.langYml.getMessage("invalid-item"))
                return@CommandHandler
            }
            var message = plugin.langYml.getMessage("give-success")
            message = message.replace("%item%", weapon.id).replace("%recipient%", receiver.name)
            sender.sendMessage(message)
            if (args.size == 3) {
                amount = args[2].toIntOrNull() ?: 1
            }
            val item: ItemStack = if (itemID.contains("fuel")) weapon.fuelItem else weapon.itemStack
            item.amount = amount
            receiver.inventory.addItem(item)
        }
    }

    override fun getTabCompleter(): TabCompleteHandler {
        return TabCompleteHandler { _, args ->
            val completions = mutableListOf<String>()

            if (args.isEmpty()) {
                return@TabCompleteHandler Weapons.values().map { it.id }
            }

            if (args.size == 1) {
                StringUtil.copyPartialMatches(
                    args[0],
                    Bukkit.getOnlinePlayers().map { it.name },
                    completions)
                return@TabCompleteHandler completions
            }

            if (args.size == 2) {
                val weaponNames = Weapons.values().map { it.id } union
                        Weapons.values().mapNotNull { if (it.fuelEnabled) "${it.id}_fuel" else null }

                StringUtil.copyPartialMatches(args[1], weaponNames, completions)
                completions.sort()
                return@TabCompleteHandler completions
            }

            if (args.size == 3) {
                StringUtil.copyPartialMatches(args[2], numbers, completions)
                return@TabCompleteHandler completions
            }

            emptyList()
        }
    }
}
