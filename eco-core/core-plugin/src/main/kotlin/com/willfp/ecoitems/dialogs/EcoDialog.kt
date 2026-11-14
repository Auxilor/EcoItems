package com.willfp.ecoitems.dialogs

import com.willfp.ecoitems.plugin
import com.willfp.ecoitems.util.dispatchAsConsole
import com.willfp.ecoitems.util.dispatchAsPlayer
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.event.ClickCallback
import org.bukkit.entity.Player
import wiki.ecohub.config.Config
import wiki.ecohub.core.plugin.startup.Prerequisite
import wiki.ecohub.core.registry.KRegistrable
import wiki.ecohub.ui.text.StringUtils
import wiki.ecohub.ui.text.formatEco
import java.util.Objects

/**
 * Paper added the dialog API in 1.21.7, and Spigot has none.
 */
private val isSupported: Boolean
    get() = Prerequisite.HAS_PAPER.isMet && Prerequisite.hasVersion(1, 21, 7).isMet

/**
 * A native Minecraft dialog screen (Paper 1.21.7+ servers): a title, body
 * text, and action buttons that run commands. Configs load everywhere;
 * opening refuses where the API doesn't exist.
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
    fun open(player: Player): Boolean {
        if (!isSupported) {
            plugin.logger.warning("Dialogs need Paper 1.21.7+")
            return false
        }

        Paper.open(this, player)
        return true
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
            val base = DialogBase.builder(
                dialog.title.toComponent()
            )
                .canCloseWithEscape(dialog.canCloseWithEscape)
                .body(dialog.body.map {
                    DialogBody.plainMessage(it.toComponent())
                })
                .build()

            val type = if (dialog.buttons.isEmpty()) {
                DialogType.notice()
            } else {
                DialogType.multiAction(
                    dialog.buttons.map { actionButton(it) }
                ).build()
            }

            val built = Dialog.create { factory ->
                factory.empty().base(base).type(type)
            }

            player.showDialog(built)
        }

        private fun actionButton(
            button: DialogButton
        ): ActionButton {
            val action = DialogAction.customClick(
                { _, audience ->
                    val player = audience as? Player ?: return@customClick
                    for (command in button.commands) {
                        plugin.dispatchAsPlayer(player, command.replace("%player%", player.name))
                    }
                    for (command in button.consoleCommands) {
                        plugin.dispatchAsConsole(command.replace("%player%", player.name))
                    }
                },
                ClickCallback.Options.builder().build()
            )

            return ActionButton.builder(button.label.toComponent())
                .tooltip(button.tooltip?.toComponent())
                .action(action)
                .build()
        }

        private fun String.toComponent() = formatEco().let {
            StringUtils.toComponent(it)
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
