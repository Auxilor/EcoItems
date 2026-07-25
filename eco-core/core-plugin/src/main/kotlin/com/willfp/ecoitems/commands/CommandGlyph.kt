package com.willfp.ecoitems.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecoitems.glyphs.Glyphs
import com.willfp.ecoitems.pack.PackFeatures
import com.willfp.ecoitems.plugin
import org.bukkit.command.CommandSender
import org.bukkit.util.StringUtil

object CommandGlyph : Subcommand(
    plugin,
    "glyph",
    "ecoitems.command.glyph",
    false
) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        val feature = PackFeatures.instance
        if (feature == null) {
            sender.sendMessage(plugin.langYml.getMessage("glyphs-require-paid"))
            return
        }

        val id = args.firstOrNull()?.lowercase() ?: run {
            sender.sendMessage(plugin.langYml.getMessage("invalid-glyph"))
            return
        }

        val chars = feature.glyphChars(id)
        if (chars == null) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-glyph"))
            return
        }

        sender.sendMessage(
            plugin.langYml.getMessage("glyph-message")
                .replace("%glyph%", id)
                .replace("%char%", chars)
        )
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        if (args.size == 1) {
            return StringUtil.copyPartialMatches(args[0], Glyphs.values().map { it.id }, mutableListOf())
        }

        return emptyList()
    }
}
