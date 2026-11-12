package com.willfp.ecoitems.commands

import com.willfp.ecoitems.glyphs.Glyphs
import com.willfp.ecoitems.pack.PackFeatures
import org.bukkit.command.CommandSender
import wiki.ecohub.core.command.CommandBuilder

internal fun CommandBuilder<CommandSender>.glyphCommand() = subcommand(
    name = "glyph",
    permission = "ecoitems.command.glyph"
) {
    executes {
        val feature = PackFeatures.instance ?: fail("glyphs-require-paid")

        val id = args.firstOrNull()?.lowercase() ?: fail("invalid-glyph")

        val chars = feature.glyphChars(id) ?: fail("invalid-glyph")

        sender.sendMessage(
            plugin.langYml.getMessage("glyph-message")
                .replace(oldValue = "%glyph%", newValue = id)
                .replace(oldValue = "%char%", newValue = chars)
        )
    }

    completes {
        if (args.size == 1) Glyphs.values().map { it.id } else emptyList()
    }
}
