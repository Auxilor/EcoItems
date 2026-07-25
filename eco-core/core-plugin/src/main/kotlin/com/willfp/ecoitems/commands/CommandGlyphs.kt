package com.willfp.ecoitems.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecoitems.pack.PackFeatures
import com.willfp.ecoitems.plugin
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object CommandGlyphs : Subcommand(
    plugin,
    "glyphs",
    "ecoitems.command.glyphs",
    true
) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        val player = sender as Player

        val feature = PackFeatures.instance
        if (feature == null) {
            sender.sendMessage(plugin.langYml.getMessage("glyphs-require-paid"))
            return
        }

        if (!feature.openGlyphPicker(player)) {
            sender.sendMessage(plugin.langYml.getMessage("no-glyphs"))
        }
    }
}
