package com.willfp.ecoitems.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecoitems.huds.Huds
import com.willfp.ecoitems.pack.PackFeatures
import com.willfp.ecoitems.plugin
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.util.StringUtil

object CommandHud : Subcommand(
    plugin,
    "hud",
    "ecoitems.command.hud",
    true
) {
    init {
        this.addSubcommand(CommandHudToggle)
    }

    override fun onExecute(sender: CommandSender, args: List<String>) {
        sender.sendMessage(plugin.langYml.getMessage("invalid-command"))
    }
}

object CommandHudToggle : Subcommand(
    plugin,
    "toggle",
    "ecoitems.command.hud.toggle",
    true
) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        val player = sender as Player

        val feature = PackFeatures.instance
        if (feature == null) {
            player.sendMessage(plugin.langYml.getMessage("huds-require-paid"))
            return
        }

        val id = args.firstOrNull()?.lowercase() ?: run {
            player.sendMessage(plugin.langYml.getMessage("invalid-hud"))
            return
        }

        val enabled = feature.toggleHud(player, id)
        if (enabled == null) {
            player.sendMessage(plugin.langYml.getMessage("invalid-hud"))
            return
        }

        player.sendMessage(
            plugin.langYml.getMessage(if (enabled) "hud-toggled-on" else "hud-toggled-off")
                .replace("%hud%", id)
        )
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        if (args.size == 1) {
            return StringUtil.copyPartialMatches(args[0], Huds.values().map { it.id }, mutableListOf())
        }

        return emptyList()
    }
}
