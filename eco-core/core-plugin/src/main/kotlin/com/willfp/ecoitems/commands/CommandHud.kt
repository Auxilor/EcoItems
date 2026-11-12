package com.willfp.ecoitems.commands

import com.willfp.ecoitems.huds.Huds
import com.willfp.ecoitems.pack.PackFeatures
import org.bukkit.command.CommandSender
import wiki.ecohub.core.command.CommandBuilder

internal fun CommandBuilder<CommandSender>.hudCommand() = playerSubcommand(
    name = "hud",
    permission = "ecoitems.command.hud"
) {
    subcommand(name = "toggle", permission = "ecoitems.command.hud.toggle") {
        executes {
            val feature = PackFeatures.instance ?: fail("huds-require-paid")

            val id = args.firstOrNull()?.lowercase() ?: fail("invalid-hud")

            val enabled = feature.toggleHud(sender, id) ?: fail("invalid-hud")

            sender.sendMessage(
                plugin.langYml.getMessage(if (enabled) "hud-toggled-on" else "hud-toggled-off")
                    .replace(oldValue = "%hud%", newValue = id)
            )
        }

        completes {
            if (args.size == 1) Huds.values().map { it.id } else emptyList()
        }
    }
}
