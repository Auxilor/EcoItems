package com.willfp.ecoitems.commands

import com.willfp.ecoitems.items.EcoItems
import com.willfp.ecoitems.premium.PremiumFeatures
import org.bukkit.command.CommandSender
import wiki.ecohub.core.command.CommandBuilder
import wiki.ecohub.core.stdlib.toNiceString
import wiki.ecohub.ui.text.StringUtils

internal fun CommandBuilder<CommandSender>.reloadCommand() = subcommand(
    name = "reload",
    permission = "ecoitems.command.reload"
) {
    executes {
        sender.sendMessage(
            plugin.langYml.getMessage("reloaded", StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                .replace(oldValue = "%time%", newValue = plugin.reloadWithTime().toNiceString())
                .replace(oldValue = "%count%", newValue = EcoItems.values().size.toString())
        )
        PremiumFeatures.notify(sender)
    }
}
