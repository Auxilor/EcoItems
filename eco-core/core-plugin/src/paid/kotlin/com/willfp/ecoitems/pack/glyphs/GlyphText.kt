package com.willfp.ecoitems.pack.glyphs

import com.willfp.eco.core.integrations.placeholder.PlaceholderIntegration
import com.willfp.ecoitems.pack.PackSettings
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Replaces glyph placeholders (":heart:", "<3") and handles raw glyph
 * characters in text. Caches are rebuilt on reload; every replacement path
 * fast-exits when the text can't contain a glyph, since the placeholder
 * integration runs on all eco-formatted text.
 */
object GlyphText {
    private class Entry(
        val assigned: AssignedGlyph,
        val pattern: Pattern,
        val literals: List<String>
    )

    private var entries: List<Entry> = emptyList()
    private var charToGlyph: Map<String, AssignedGlyph> = emptyMap()
    private var unescapePattern: Pattern? = null

    var assignments: Map<String, AssignedGlyph> = emptyMap()
        private set

    var formatChat = true
        private set
    var formatSigns = true
        private set
    var tabComplete = true
        private set

    fun reload(assignments: Map<String, AssignedGlyph>, settings: PackSettings) {
        this.assignments = assignments
        this.formatChat = settings.glyphsFormatChat
        this.formatSigns = settings.glyphsFormatSigns
        this.tabComplete = settings.glyphsTabComplete

        // Longest placeholders first so e.g. ":hearts:" wins over ":heart:".
        entries = assignments.values
            .filter { it.glyph.placeholders.isNotEmpty() }
            .sortedByDescending { it.glyph.placeholders.maxOf { placeholder -> placeholder.length } }
            .map { assigned ->
                val alternation = assigned.glyph.placeholders.joinToString("|") { Pattern.quote(it) }
                Entry(
                    assigned,
                    Pattern.compile("(?<!\\\\)(?:$alternation)"),
                    assigned.glyph.placeholders
                )
            }

        charToGlyph = buildMap {
            for (assigned in assignments.values) {
                for (frame in assigned.frames) {
                    put(String(Character.toChars(frame)), assigned)
                }
            }
        }

        unescapePattern = assignments.values
            .flatMap { it.glyph.placeholders }
            .takeIf { it.isNotEmpty() }
            ?.joinToString("|") { Pattern.quote(it) }
            ?.let { Pattern.compile("\\\\($it)") }
    }

    fun clear() {
        entries = emptyList()
        charToGlyph = emptyMap()
        unescapePattern = null
        assignments = emptyMap()
    }

    fun hasPermission(player: Player, glyph: com.willfp.ecoitems.glyphs.Glyph): Boolean =
        glyph.permission.isEmpty() || player.hasPermission(glyph.permission)

    fun shiftString(pixels: Int): String = ShiftChars.shift(pixels)

    /** The raw (uncolored) characters for a glyph; animated glyphs need [legacyChars] to animate. */
    fun rawChars(assigned: AssignedGlyph): String = buildString {
        for ((index, frame) in assigned.frames.withIndex()) {
            appendCodePoint(frame)
            if (index < assigned.frames.lastIndex) {
                appendCodePoint(assigned.reset!!)
            }
        }
        val animation = assigned.glyph.animation
        if (animation != null && animation.offset != 0) {
            appendCodePoint(assigned.offsetChar!!)
        }
    }

    /** The legacy-format text for a glyph, with magic frame colors for animated ones. */
    fun legacyChars(assigned: AssignedGlyph, restore: String): String {
        val animation = assigned.glyph.animation
            ?: return if (assigned.glyph.colorable) {
                rawChars(assigned)
            } else {
                "§f${rawChars(assigned)}$restore"
            }

        return buildString {
            for ((index, frame) in assigned.frames.withIndex()) {
                append(legacyHex(GlyphColors.rgb(animation, index)))
                appendCodePoint(frame)
                if (index < assigned.frames.lastIndex) {
                    appendCodePoint(assigned.reset!!)
                }
            }
            if (animation.offset != 0) {
                appendCodePoint(assigned.offsetChar!!)
            }
            append(restore)
        }
    }

    private fun legacyHex(rgb: Int): String = buildString {
        append("§x")
        for (char in "%06x".format(rgb)) {
            append('§').append(char)
        }
    }

    private fun mightContain(text: String): Boolean {
        if (entries.isEmpty()) {
            return false
        }
        return entries.any { entry -> entry.literals.any { text.contains(it) } } ||
            charToGlyph.keys.any { text.contains(it) }
    }

    /**
     * Replaces placeholders in legacy-format text. With [checkPermissions],
     * unpermitted placeholders stay literal and unpermitted raw glyph
     * characters are stripped.
     */
    fun replaceLegacy(text: String, player: Player?, checkPermissions: Boolean): String {
        if (!mightContain(text)) {
            return text
        }

        var result = text

        // Raw characters first, so freshly inserted glyphs aren't re-processed.
        if (checkPermissions && player != null) {
            for ((chars, assigned) in charToGlyph) {
                if (result.contains(chars) && !hasPermission(player, assigned.glyph)) {
                    result = result.replace(chars, "")
                }
            }
        }

        for (entry in entries) {
            if (entry.literals.none { result.contains(it) }) {
                continue
            }
            if (checkPermissions && player != null && !hasPermission(player, entry.assigned.glyph)) {
                continue
            }

            val input = result
            result = entry.pattern.matcher(input).replaceAll { match ->
                val restore = ChatColor.getLastColors(input.substring(0, match.start())).ifEmpty { "§r" }
                Matcher.quoteReplacement(legacyChars(entry.assigned, restore))
            }
        }

        unescapePattern?.let {
            result = it.matcher(result).replaceAll("$1")
        }

        return result
    }

    /** Replaces placeholders and handles raw glyph characters in a chat component (Paper). */
    fun replaceComponent(message: Component, player: Player): Component {
        var result = message

        // Raw characters first: unpermitted ones are re-fonted to
        // minecraft:random so they render scrambled.
        for ((chars, assigned) in charToGlyph) {
            if (!hasPermission(player, assigned.glyph)) {
                result = result.replaceText(
                    TextReplacementConfig.builder()
                        .matchLiteral(chars)
                        .replacement(Component.text(chars).font(Key.key("minecraft", "random")))
                        .build()
                )
            }
        }

        for (entry in entries) {
            if (!hasPermission(player, entry.assigned.glyph)) {
                continue
            }
            result = result.replaceText(
                TextReplacementConfig.builder()
                    .match(entry.pattern)
                    .replacement(glyphComponent(entry.assigned))
                    .build()
            )
        }

        unescapePattern?.let { pattern ->
            result = result.replaceText(
                TextReplacementConfig.builder()
                    .match(pattern)
                    .replacement { match, _ -> Component.text(match.group(1)) }
                    .build()
            )
        }

        return result
    }

    private fun glyphComponent(assigned: AssignedGlyph): Component {
        val animation = assigned.glyph.animation

        if (animation == null) {
            val text = Component.text(rawChars(assigned))
            return if (assigned.glyph.colorable) text else text.color(NamedTextColor.WHITE)
        }

        var component = Component.empty()
        for ((index, frame) in assigned.frames.withIndex()) {
            var framePart = Component.text(String(Character.toChars(frame)))
                .color(TextColor.color(GlyphColors.rgb(animation, index)))
                .shadowColor(ShadowColor.none())
            component = component.append(framePart)
            if (index < assigned.frames.lastIndex) {
                component = component.append(Component.text(String(Character.toChars(assigned.reset!!))))
            }
        }
        if (animation.offset != 0) {
            component = component.append(Component.text(String(Character.toChars(assigned.offsetChar!!))))
        }
        return component
    }

    object GlyphPlaceholderIntegration : PlaceholderIntegration {
        override fun getPluginName(): String = "EcoItemsGlyphs"

        override fun registerIntegration() {
            // Registration is handled by PlaceholderManager.addIntegration.
        }

        override fun translate(text: String, player: Player?): String {
            return replaceLegacy(text, player, checkPermissions = false)
        }
    }
}
