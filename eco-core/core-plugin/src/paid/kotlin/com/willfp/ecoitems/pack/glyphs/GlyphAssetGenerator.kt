package com.willfp.ecoitems.pack.glyphs

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.willfp.ecoitems.EcoItemsPlugin
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Generates the font providers, shift font, and (for animated glyphs) the
 * text core shaders. Runs after the pack/assets overlay copy so a
 * user-supplied minecraft:default font is merged rather than clobbered.
 */
object GlyphAssetGenerator {
    private const val DEFAULT_FONT = "assets/minecraft/font/default.json"
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    private val SHADER_VARIANTS = listOf(
        "rendertype_text",
        "rendertype_text_see_through",
        "rendertype_text_intensity",
        "rendertype_text_intensity_see_through"
    )

    /**
     * The glyph bitmap providers plus the space provider (shifts + animated
     * resets), for composing into fonts. Fresh json instances per call.
     */
    internal fun buildProviders(
        plugin: EcoItemsPlugin,
        glyphs: Collection<AssignedGlyph>
    ): Pair<JsonArray, JsonObject> {
        val providers = JsonArray()
        val advances = JsonObject()

        for ((codepoint, advance) in ShiftChars.advances.toSortedMap()) {
            advances.addProperty(String(Character.toChars(codepoint)), advance)
        }

        for (assigned in glyphs.sortedBy { it.glyph.id }) {
            addGlyph(plugin, assigned, providers, advances)
        }

        val spaceProvider = JsonObject()
        spaceProvider.addProperty("type", "space")
        spaceProvider.add("advances", advances)

        return providers to spaceProvider
    }

    fun generate(
        plugin: EcoItemsPlugin,
        glyphs: Collection<AssignedGlyph>,
        entries: MutableMap<String, ByteArray>
    ) {
        val (providers, spaceProvider) = buildProviders(plugin, glyphs)

        // The standalone shift font, for components that want an explicit font.
        val shiftFont = JsonObject()
        val shiftProviders = JsonArray()
        val shiftOnly = JsonObject()
        shiftOnly.addProperty("type", "space")
        val shiftAdvances = JsonObject()
        for ((codepoint, advance) in ShiftChars.advances.toSortedMap()) {
            shiftAdvances.addProperty(String(Character.toChars(codepoint)), advance)
        }
        shiftOnly.add("advances", shiftAdvances)
        shiftProviders.add(shiftOnly)
        shiftFont.add("providers", shiftProviders)
        entries["assets/ecoitems/font/shift.json"] = gson.toJson(shiftFont).encodeToByteArray()

        // Merge into minecraft:default - user-supplied providers first, since
        // the first provider defining a character wins.
        val font = JsonObject()
        val merged = JsonArray()
        entries[DEFAULT_FONT]?.let { existing ->
            runCatching { JsonParser.parseString(existing.decodeToString()).asJsonObject }
                .getOrNull()
                ?.getAsJsonArray("providers")
                ?.forEach { merged.add(it) }
                ?: plugin.logger.warning("pack/assets/minecraft/font/default.json is not a valid font file; replacing it")
        }
        providers.forEach { merged.add(it) }
        merged.add(spaceProvider)
        font.add("providers", merged)
        entries[DEFAULT_FONT] = gson.toJson(font).encodeToByteArray()

        val animated = glyphs.filter { it.glyph.isAnimated }
        if (animated.isNotEmpty()) {
            generateShaders(plugin, animated, entries)
        }
    }

    private fun addGlyph(
        plugin: EcoItemsPlugin,
        assigned: AssignedGlyph,
        providers: JsonArray,
        advances: JsonObject
    ) {
        val glyph = assigned.glyph
        val animation = glyph.animation

        val textureRef: String
        var textureFile: File? = null

        if (":" in glyph.texture) {
            textureRef = if (glyph.texture.endsWith(".png")) glyph.texture else "${glyph.texture}.png"
        } else {
            textureFile = plugin.dataFolder.resolve("pack/glyphs/${glyph.texture}.png")
            if (!textureFile.exists()) {
                plugin.logger.warning(
                    "Skipping glyph ${glyph.id}: texture file pack/glyphs/${glyph.texture}.png does not exist"
                )
                return
            }
            textureRef = "ecoitems:glyphs/${glyph.texture}.png"
        }

        if (glyph.ascent > glyph.height) {
            plugin.logger.warning("Skipping glyph ${glyph.id}: ascent (${glyph.ascent}) is larger than height (${glyph.height})")
            return
        }

        val bitmap = JsonObject()
        bitmap.addProperty("type", "bitmap")
        bitmap.addProperty("file", textureRef)
        bitmap.addProperty("ascent", glyph.ascent)
        bitmap.addProperty("height", glyph.height)
        val chars = JsonArray()

        if (animation == null) {
            chars.add(String(Character.toChars(assigned.frames.single())))
            bitmap.add("chars", chars)
            providers.add(bitmap)
            return
        }

        // Animated: figure out the sprite sheet layout from the png header.
        val dimensions = textureFile?.let { pngDimensions(it) }
        if (dimensions == null) {
            plugin.logger.warning("Skipping animated glyph ${glyph.id}: could not read the sprite sheet dimensions")
            return
        }

        val (width, height) = dimensions
        val frameWidth: Int
        val frameHeight: Int

        when {
            height % animation.frames == 0 && height / animation.frames <= width -> {
                // Vertical strip: one char-row per frame.
                frameWidth = width
                frameHeight = height / animation.frames
                for (frame in assigned.frames) {
                    chars.add(String(Character.toChars(frame)))
                }
            }

            width % animation.frames == 0 -> {
                // Horizontal strip: one row with every frame char.
                frameWidth = width / animation.frames
                frameHeight = height
                chars.add(buildString {
                    for (frame in assigned.frames) {
                        appendCodePoint(frame)
                    }
                })
            }

            else -> {
                plugin.logger.warning(
                    "Skipping animated glyph ${glyph.id}: sprite sheet is ${width}x$height, which doesn't divide into ${animation.frames} frames"
                )
                return
            }
        }

        bitmap.add("chars", chars)
        providers.add(bitmap)

        // Reset char: negative advance pulling the cursor back one frame width,
        // so consecutive frames render stacked at the same position.
        val renderedWidth = max(1, (glyph.height * frameWidth.toFloat() / frameHeight).roundToInt())
        advances.addProperty(String(Character.toChars(assigned.reset!!)), -renderedWidth)

        if (animation.offset != 0) {
            advances.addProperty(String(Character.toChars(assigned.offsetChar!!)), animation.offset)
        }
    }

    private fun generateShaders(
        plugin: EcoItemsPlugin,
        animated: List<AssignedGlyph>,
        entries: MutableMap<String, ByteArray>
    ) {
        val constants = animConstants(animated)

        fun write(template: String, target: String) {
            val resource = javaClass.getResourceAsStream("/glyph-shaders/$template")
            if (resource == null) {
                plugin.logger.warning("Missing bundled shader template glyph-shaders/$template")
                return
            }
            val content = resource.use { it.readBytes().decodeToString() }
            entries[target] = content.replace("%ANIM_CONFIGS%", constants).encodeToByteArray()
        }

        for (variant in SHADER_VARIANTS) {
            for (extension in listOf("vsh", "fsh", "json")) {
                write("base/$variant.$extension", "assets/minecraft/shaders/core/$variant.$extension")
                write(
                    "base/$variant.$extension",
                    "overlay_pre_26/assets/minecraft/shaders/core/$variant.$extension"
                )
            }
            for (extension in listOf("vsh", "fsh")) {
                write("overlay_26/$variant.$extension", "overlay_26/assets/minecraft/shaders/core/$variant.$extension")
            }
        }

        write("overlay_26_2/text.vsh", "overlay_26_2/assets/minecraft/shaders/core/text.vsh")
        write("overlay_26_2/text.fsh", "overlay_26_2/assets/minecraft/shaders/core/text.fsh")
    }

    /**
     * The shader whitelist of (green channel, frame count) pairs, so ordinary
     * #FExxxx gradient colors aren't mistaken for animation markers.
     */
    private fun animConstants(animated: List<AssignedGlyph>): String {
        val configs = linkedSetOf<Pair<Int, Int>>()
        for (assigned in animated) {
            val animation = assigned.glyph.animation!!
            configs.add(GlyphColors.greenChannel(animation) to animation.frames)
        }

        val builder = StringBuilder()
        builder.appendLine("const int ECOITEMS_ANIM_CONFIG_COUNT = ${configs.size};")
        builder.appendLine("const ivec2 ECOITEMS_ANIM_CONFIGS[${max(1, configs.size)}] = ivec2[](")
        if (configs.isEmpty()) {
            builder.appendLine("    ivec2(0, 0)")
        } else {
            builder.appendLine(configs.joinToString(",\n") { (green, frames) -> "    ivec2($green, $frames)" })
        }
        builder.append(");")
        return builder.toString()
    }

    /** Reads png dimensions from the IHDR chunk without decoding the image. */
    private fun pngDimensions(file: File): Pair<Int, Int>? {
        val header = ByteArray(24)
        file.inputStream().use { input ->
            if (input.read(header) < 24) {
                return null
            }
        }

        // PNG magic + "IHDR" at offset 12.
        if (header[1].toInt() != 'P'.code || header[12].toInt() != 'I'.code) {
            return null
        }

        fun int(offset: Int): Int =
            ((header[offset].toInt() and 0xFF) shl 24) or
                ((header[offset + 1].toInt() and 0xFF) shl 16) or
                ((header[offset + 2].toInt() and 0xFF) shl 8) or
                (header[offset + 3].toInt() and 0xFF)

        return int(16) to int(20)
    }
}
