package com.willfp.ecoitems.dialogs

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.registry.KRegistrable
import com.willfp.eco.util.formatEco
import com.willfp.eco.util.toComponent
import com.willfp.ecoitems.plugin
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.Objects

/**
 * A native Minecraft dialog screen (1.21.6+ clients, Paper servers): a
 * title, body text, and action buttons that run commands. Configs load
 * everywhere; opening quietly refuses where the API doesn't exist.
 */
class EcoDialog(
    override val id: String,
    val config: Config
) : KRegistrable {
    val title: String = config.getFormattedString("title")

    val body: List<String> = config.getFormattedStrings("body")

    val canCloseWithEscape = config.getBoolOrNull("can-close-with-escape") ?: true

    val buttons = config.getSubsections("buttons").map { button ->
        DialogButton(
            button.getFormattedString("label"),
            button.getFormattedStringOrNull("tooltip"),
            button.getStrings("commands"),
            button.getStrings("console-commands")
        )
    }

    /** Opens the dialog; false when the platform can't show dialogs. */
    fun open(player: Player): Boolean = runCatching {
        Paper.open(this, player)
        true
    }.getOrElse {
        plugin.logger.warning("Dialogs need Paper 1.21.6+ ($it)")
        false
    }

    class DialogButton(
        val label: String,
        val tooltip: String?,
        val commands: List<String>,
        val consoleCommands: List<String>
    )

    /** The only object touching the (Paper-only) dialog API. */
    @Suppress("UnstableApiUsage")
    private object Paper {
        fun open(dialog: EcoDialog, player: Player) {
            val base = io.papermc.paper.registry.data.dialog.DialogBase.builder(
                dialog.title.toComponent()
            )
                .canCloseWithEscape(dialog.canCloseWithEscape)
                .body(dialog.body.map {
                    io.papermc.paper.registry.data.dialog.body.DialogBody.plainMessage(it.toComponent())
                })
                .build()

            val type = if (dialog.buttons.isEmpty()) {
                io.papermc.paper.registry.data.dialog.type.DialogType.notice()
            } else {
                io.papermc.paper.registry.data.dialog.type.DialogType.multiAction(
                    dialog.buttons.map { actionButton(it) }
                ).build()
            }

            val built = io.papermc.paper.dialog.Dialog.create { factory ->
                factory.empty().base(base).type(type)
            }

            player.showDialog(built)
        }

        private fun actionButton(
            button: DialogButton
        ): io.papermc.paper.registry.data.dialog.ActionButton {
            val action = io.papermc.paper.registry.data.dialog.action.DialogAction.customClick(
                { _, audience ->
                    val player = audience as? Player ?: return@customClick
                    for (command in button.commands) {
                        Bukkit.dispatchCommand(player, command.replace("%player%", player.name))
                    }
                    for (command in button.consoleCommands) {
                        Bukkit.dispatchCommand(
                            Bukkit.getConsoleSender(),
                            command.replace("%player%", player.name)
                        )
                    }
                },
                net.kyori.adventure.text.event.ClickCallback.Options.builder().build()
            )

            return io.papermc.paper.registry.data.dialog.ActionButton.builder(button.label.toComponent())
                .tooltip(button.tooltip?.toComponent())
                .action(action)
                .build()
        }

        private fun String.toComponent() = formatEco().let {
            com.willfp.eco.util.StringUtils.toComponent(it)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is EcoDialog) {
            return false
        }

        return this.id == other.id
    }

    override fun hashCode(): Int {
        return Objects.hash(this.id)
    }

    override fun toString(): String {
        return "EcoDialog{$id}"
    }
}
