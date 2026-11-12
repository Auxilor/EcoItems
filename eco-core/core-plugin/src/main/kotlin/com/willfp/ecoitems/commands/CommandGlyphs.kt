package com.willfp.ecoitems.commands

import com.willfp.ecoitems.pack.PackFeatures
import org.bukkit.command.CommandSender
import wiki.ecohub.core.command.CommandBuilder

internal fun CommandBuilder<CommandSender>.glyphsCommand() = playerSubcommand(
    name = "glyphs",
    permission = "ecoitems.command.glyphs"
) {
    executes {
        val feature = PackFeatures.instance ?: fail("glyphs-require-paid")

        if (!feature.openGlyphPicker(sender)) {
            reply("no-glyphs")
        }
    }
}
