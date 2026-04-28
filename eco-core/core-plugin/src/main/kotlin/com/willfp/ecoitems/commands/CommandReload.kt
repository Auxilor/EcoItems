package com.willfp.ecoitems.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.core.recipe.Recipes
import com.willfp.eco.util.StringUtils
import com.willfp.eco.util.toNiceString
import com.willfp.ecoitems.items.EcoItems
import com.willfp.ecoitems.plugin
import org.bukkit.command.CommandSender
import org.bukkit.inventory.Recipe

object CommandReload : Subcommand(plugin, "reload", "ecoitems.command.reload", false) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        sender.sendMessage(
            plugin.langYml.getMessage("reloaded", StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                .replace("%time%", plugin.reloadWithTime().toNiceString())
                .replace("%count%", EcoItems.values().size.toString())
        )
    }
}