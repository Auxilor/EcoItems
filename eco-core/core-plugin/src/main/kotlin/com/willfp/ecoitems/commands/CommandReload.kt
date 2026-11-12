package com.willfp.ecoitems.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecoitems.items.EcoItems
import com.willfp.ecoitems.plugin
import com.willfp.ecoitems.premium.PremiumFeatures
import org.bukkit.command.CommandSender
import org.bukkit.inventory.Recipe
import wiki.ecohub.core.stdlib.toNiceString
import wiki.ecohub.modules.recipe.Recipes
import wiki.ecohub.ui.text.StringUtils

object CommandReload : Subcommand(plugin, "reload", "ecoitems.command.reload", false) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        sender.sendMessage(
            plugin.langYml.getMessage("reloaded", StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                .replace("%time%", plugin.reloadWithTime().toNiceString())
                .replace("%count%", EcoItems.values().size.toString())
        )
        PremiumFeatures.notify(sender)
    }
}