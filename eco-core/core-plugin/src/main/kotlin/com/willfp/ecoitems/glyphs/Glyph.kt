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

    /** If the glyph is offered in the /ecoitems glyphs picker book. */
    val showInPicker = config.getBoolOrNull("show-in-picker") ?: true

    /** If surrounding text color should tint the glyph (default renders white). */
    val colorable = config.getBool("colorable")

    val animation: GlyphAnimation? = run {
        // A .gif texture animates by itself; explicit config overrides it.
        val gif = GifTexture.meta(GifTexture.file(texture))
        when {
            config.has("animation") -> GlyphAnimation.fromConfig(id, config.getSubsection("animation"), gif)
            gif != null -> GlyphAnimation(id, gif.frames, gif.fps, loop = true, offset = 0)
            else -> null
        }
    }

    /** When set, this glyph is one cell of a shared sprite sheet. */
    val bitmap: GlyphBitmap? = if (config.has("bitmap")) {
        GlyphBitmap(id, config.getSubsection("bitmap"))
    } else {
        null
    }

    val isAnimated: Boolean
        get() = animation != null

    init {
        if (bitmap != null && animation != null) {
            plugin.logger.warning("Glyph $id is both animated and a bitmap cell; the bitmap is ignored")
        }
    }

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

/**
 * One cell of a shared sprite sheet: glyphs with the same sheet (texture,
 * rows, columns) plus matching ascent/height are emitted as a single grid
 * font provider.
 */
class GlyphBitmap(id: String, config: Config) {
    /** The shared sheet texture; overrides the glyph's own texture. */
    val texture: String = config.getString("texture")

    val rows = config.getIntOrNull("rows") ?: 1
    val columns = config.getIntOrNull("columns") ?: 1

    /** This glyph's cell, zero-indexed. */
    val row = config.getIntOrNull("row") ?: 0
    val column = config.getIntOrNull("column") ?: 0

    init {
        if (row !in 0 until rows || column !in 0 until columns) {
            plugin.logger.warning(
                "Glyph $id's bitmap cell ($row, $column) is outside its ${rows}x$columns sheet"
            )
        }
    }
}

class GlyphAnimation(
    id: String,
    frames: Int,
    fps: Int,
    val loop: Boolean,
    /** Extra pixels to advance after the glyph, e.g. to add spacing. */
    val offset: Int
) {
    // The shader encodes the frame index and count in 4 bits each, and the
    // fps in 7 bits, so these limits are hard.
    val frames = frames.coerceIn(1, MAX_FRAMES)

    val fps = fps.coerceIn(1, MAX_FPS)

    init {
        if (frames > MAX_FRAMES) {
            plugin.logger.warning(
                "Glyph $id has more than $MAX_FRAMES animation frames; clamped to $MAX_FRAMES"
            )
        }
    }

    companion object {
        const val MAX_FRAMES = 16
        const val MAX_FPS = 127

        /** From an animation: section; a gif texture supplies the defaults. */
        fun fromConfig(id: String, config: Config, gif: GifTexture.Meta?): GlyphAnimation =
            GlyphAnimation(
                id,
                config.getIntOrNull("frames") ?: gif?.frames ?: 1,
                config.getIntOrNull("fps") ?: gif?.fps ?: 1,
                config.getBoolOrNull("loop") ?: true,
                config.getIntOrNull("offset") ?: 0
            )
    }
}
