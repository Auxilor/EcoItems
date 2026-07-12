package com.willfp.ecoitems.glyphs

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.registry.KRegistrable
import com.willfp.ecoitems.plugin
import java.util.Objects

class Glyph(
    override val id: String,
    val config: Config
) : KRegistrable {
    val texture: String = config.getString("texture")

    val ascent = config.getInt("ascent")

    val height = config.getInt("height")

    /** The explicit character, or null to auto-assign a stable codepoint. */
    val configuredChar: String? = config.getStringOrNull("char")?.takeIf { it.isNotEmpty() }

    /** Chat placeholders that insert this glyph, e.g. ":heart:" or "<3". */
    val placeholders: List<String> = config.getStrings("placeholders")

    /** Empty = usable by everyone. Checked in chat/signs/tab-complete only. */
    val permission: String = config.getString("permission")

    val tabComplete = config.getBool("tab-complete")

    /** If surrounding text color should tint the glyph (default renders white). */
    val colorable = config.getBool("colorable")

    val animation: GlyphAnimation? = if (config.has("animation")) {
        GlyphAnimation(id, config.getSubsection("animation"))
    } else {
        null
    }

    val isAnimated: Boolean
        get() = animation != null

    override fun equals(other: Any?): Boolean {
        if (other !is Glyph) {
            return false
        }

        return this.id == other.id
    }

    override fun hashCode(): Int {
        return Objects.hash(this.id)
    }

    override fun toString(): String {
        return "Glyph{$id}"
    }
}

class GlyphAnimation(id: String, config: Config) {
    // The shader encodes the frame index and count in 4 bits each, and the
    // fps in 7 bits, so these limits are hard.
    val frames = config.getInt("frames").coerceIn(1, MAX_FRAMES)

    val fps = config.getInt("fps").coerceIn(1, MAX_FPS)

    val loop = config.getBoolOrNull("loop") ?: true

    /** Extra pixels to advance after the glyph, e.g. to add spacing. */
    val offset = config.getIntOrNull("offset") ?: 0

    init {
        if (config.getInt("frames") > MAX_FRAMES) {
            plugin.logger.warning(
                "Glyph $id has more than $MAX_FRAMES animation frames; clamped to $MAX_FRAMES"
            )
        }
    }

    companion object {
        const val MAX_FRAMES = 16
        const val MAX_FPS = 127
    }
}
