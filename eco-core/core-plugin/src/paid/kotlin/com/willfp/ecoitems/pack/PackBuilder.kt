package com.willfp.ecoitems.pack

import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.pack.glyphs.AssignedGlyph
import com.willfp.ecoitems.pack.glyphs.GlyphAssetGenerator
import com.willfp.ecoitems.sounds.Sound
import java.io.File
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object PackBuilder {
    /**
     * Assembles and writes the pack zip: generated pack.mcmeta and pack.png,
     * the generated item assets, and everything in the pack/assets overlay
     * (which wins on collisions).
     */
    fun build(
        plugin: EcoItemsPlugin,
        settings: PackSettings,
        assets: List<ItemPackAsset>,
        glyphs: Collection<AssignedGlyph>,
        sounds: Collection<Sound>
    ): BuiltPack {
        val entries = sortedMapOf<String, ByteArray>()

        val hasAnimatedGlyphs = glyphs.any { it.glyph.isAnimated }
        entries["pack.mcmeta"] = PackMcmeta.json(settings.description, hasAnimatedGlyphs).encodeToByteArray()
        entries["pack.png"] = packPng(plugin)

        // Everything in pack/textures and pack/models is available to item
        // definitions and models as ecoitems:item/<path>; glyph sheets map to
        // ecoitems:glyphs/<path> (outside the block atlas directories).
        copyTree(plugin.dataFolder.resolve("pack/textures"), "assets/ecoitems/textures/item/", entries)
        copyTree(plugin.dataFolder.resolve("pack/models"), "assets/ecoitems/models/item/", entries)
        copyTree(plugin.dataFolder.resolve("pack/glyphs"), "assets/ecoitems/textures/glyphs/", entries)
        copyTree(plugin.dataFolder.resolve("pack/sounds"), "assets/ecoitems/sounds/", entries)

        ItemAssetGenerator.generate(plugin, assets, entries)

        // The raw overlay wins on collisions with generated files.
        copyTree(plugin.dataFolder.resolve("pack/assets"), "assets/", entries)

        // After the overlay, so user-supplied font/sounds/lang files are
        // merged into the generated ones rather than replaced.
        GlyphAssetGenerator.generate(plugin, glyphs, entries)
        SoundAssetGenerator.generate(plugin, sounds, entries)
        LangAssetGenerator.generate(plugin, entries)

        return write(plugin.dataFolder.resolve("pack.zip"), entries)
    }

    private fun copyTree(directory: File, prefix: String, entries: MutableMap<String, ByteArray>) {
        if (!directory.isDirectory) {
            return
        }

        for (file in directory.walkTopDown().filter { it.isFile }) {
            entries[prefix + file.relativeTo(directory).invariantSeparatorsPath] = file.readBytes()
        }
    }

    private fun packPng(plugin: EcoItemsPlugin): ByteArray {
        val override = plugin.dataFolder.resolve("pack/pack.png")
        if (override.isFile) {
            return override.readBytes()
        }

        return checkNotNull(javaClass.getResourceAsStream("/pack/defaults/pack.png")) {
            "Bundled pack.png is missing"
        }.use { it.readBytes() }
    }

    private fun write(target: File, entries: Map<String, ByteArray>): BuiltPack {
        val digest = MessageDigest.getInstance("SHA-1")

        DigestOutputStream(target.outputStream().buffered(), digest).use { out ->
            ZipOutputStream(out).use { zip ->
                // Sorted paths and fixed timestamps keep the hash stable across
                // reloads with unchanged content, so re-uploads and client
                // re-downloads become no-ops.
                for ((path, bytes) in entries) {
                    val entry = ZipEntry(path)
                    entry.time = 0L
                    zip.putNextEntry(entry)
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
        }

        val sha1 = digest.digest().joinToString("") { "%02x".format(it) }
        return BuiltPack(target, sha1)
    }
}
