package com.willfp.ecoitems.pack.glyphs

import com.willfp.eco.util.asAudience
import com.willfp.eco.util.formatEco
import com.willfp.eco.util.toComponent
import com.willfp.ecoitems.plugin
import net.kyori.adventure.inventory.Book
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.entity.Player

/**
 * The glyph picker: a virtual book of every glyph the player may use.
 * Clicking a glyph suggests its chat placeholder (or the raw character)
 * into the player's chat box.
 */
object GlyphPicker {
    private const val PER_LINE = 5
    private const val LINES_PER_PAGE = 8

    /** Opens the book; false if the player has no usable glyphs. */
    fun open(player: Player): Boolean {
        val usable = GlyphText.assignments.values
            .filter { it.glyph.permission.isEmpty() || player.hasPermission(it.glyph.permission) }
            .sortedBy { it.glyph.id }

        if (usable.isEmpty()) {
            return false
        }

        val hoverFormat = plugin.langYml.getString("messages.glyph-picker-hover")

        val entries = usable.map { assigned ->
            val glyph = assigned.glyph
            val chars = GlyphText.rawChars(assigned)
            val placeholder = glyph.placeholders.firstOrNull() ?: chars

            val hover = hoverFormat
                .replace("%glyph%", glyph.id)
                .replace("%placeholder%", glyph.placeholders.joinToString(" ").ifEmpty { glyph.id })
                .formatEco()
                .split("\\n")
                .joinToString("\n")
                .toComponent()

            Component.text(chars)
                .hoverEvent(hover)
                .clickEvent(ClickEvent.suggestCommand(placeholder))
        }

        val pages = entries
            .chunked(PER_LINE)
            .map { line ->
                line.fold(Component.empty() as Component) { acc, entry ->
                    acc.append(entry).append(Component.text(" "))
                }
            }
            .chunked(LINES_PER_PAGE)
            .map { lines ->
                lines.fold(Component.empty() as Component) { acc, line ->
                    acc.append(line).append(Component.newline())
                }
            }

        val title = plugin.langYml.getString("messages.glyph-picker-title").formatEco().toComponent()
        player.asAudience().openBook(Book.book(title, Component.text("EcoItems"), pages))
        return true
    }
}
