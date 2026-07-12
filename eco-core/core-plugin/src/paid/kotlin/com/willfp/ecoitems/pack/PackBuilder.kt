package com.willfp.ecoitems.pack

import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.huds.Hud
import com.willfp.ecoitems.pack.glyphs.AssignedGlyph
import com.willfp.ecoitems.pack.glyphs.GlyphAssetGenerator
import com.willfp.ecoitems.pack.huds.HudFontGenerator
import com.willfp.ecoitems.sounds.Sound
import java.io.File
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object PackBuilder {
    /**
     * Assembles and writes the pack zip.
     *
     * The pack folder mirrors a vanilla resource pack: assets/<namespace>/...
     * plus pack.png, an optional pack.mcmeta (only its overlay entries are
     * used), and overlay directories - all copied as-is. The exceptions are
     * pack/imports/ (external packs, merged lowest-priority) and
     * assets/minecraft/lang/global.json (applied to every language).
     */
    fun build(
        plugin: EcoItemsPlugin,
        settings: PackSettings,
        assets: List<ItemPackAsset>,
        glyphs: Collection<AssignedGlyph>,
        sounds: Collection<Sound>,
        huds: Collection<Hud>,
        imports: ImportedPacks
    ): BuiltPack {
        val entries = sortedMapOf<String, ByteArray>()

        // External packs are the lowest-priority layer: everything EcoItems
        // generates and everything in the pack folder wins on collision, and
        // the font/sounds/lang generators merge with what the imports put here.
        entries.putAll(imports.entries)

        val userOverlays = userMcmetaOverlays(plugin)

        val hasAnimatedGlyphs = glyphs.any { it.glyph.isAnimated }
        entries["pack.mcmeta"] = PackMcmeta.json(
            settings.description,
            hasAnimatedGlyphs,
            (imports.overlays + userOverlays).map { it.toString() }
        ).encodeToByteArray()
        entries["pack.png"] = bundledPackPng()

        ItemAssetGenerator.generate(plugin, assets, entries)

        // Feature assets (currently the 2D head models) ship in every build
        // rather than being extracted to the pack folder, so fixes reach
        // existing installs; the pack folder still wins on collision.
        bundledBuiltins(plugin, entries)

        // The pack folder wins on collisions with generated files and
        // imports, except mergeable files (fonts/sounds/lang/atlases),
        // which merge with the incoming file winning.
        mergeTree(plugin.dataFolder.resolve("pack"), entries) { path ->
            path.startsWith("imports/") ||
                path == "pack.mcmeta" ||
                path == LangAssetGenerator.GLOBAL_LANG
        }

        // After the pack folder, so user-supplied font/sounds/lang files are
        // merged into the generated ones rather than replaced.
        GlyphAssetGenerator.generate(plugin, glyphs, entries)
        HudFontGenerator.generate(plugin, huds, glyphs, entries)
        SoundAssetGenerator.generate(plugin, sounds, entries)
        LangAssetGenerator.generate(plugin, entries)

        return write(plugin.dataFolder.resolve("pack.zip"), entries)
    }

    private fun userMcmetaOverlays(plugin: EcoItemsPlugin): List<com.google.gson.JsonObject> {
        val file = plugin.dataFolder.resolve("pack/pack.mcmeta")
        if (!file.isFile) {
            return emptyList()
        }

        return PackImports.overlayEntries(plugin, "pack/pack.mcmeta", file.readBytes())
    }

    private fun bundledPackPng(): ByteArray =
        checkNotNull(javaClass.getResourceAsStream("/pack/defaults/pack.png")) {
            "Bundled pack.png is missing"
        }.use { it.readBytes() }

    private fun bundledBuiltins(plugin: EcoItemsPlugin, entries: MutableMap<String, ByteArray>) {
        ZipFile(plugin.jar).use { zip ->
            for (entry in zip.entries()) {
                if (entry.isDirectory || !entry.name.startsWith("pack/builtin/")) {
                    continue
                }

                entries[entry.name.removePrefix("pack/builtin/")] =
                    zip.getInputStream(entry).use { it.readBytes() }
            }
        }
    }

    private fun mergeTree(
        directory: File,
        entries: MutableMap<String, ByteArray>,
        skip: (String) -> Boolean
    ) {
        if (!directory.isDirectory) {
            return
        }

        for (file in directory.walkTopDown().filter { it.isFile }) {
            val path = file.relativeTo(directory).invariantSeparatorsPath
            if (skip(path)) {
                continue
            }

            val incoming = file.readBytes()
            val existing = entries[path]

            entries[path] = if (existing != null) {
                PackImports.merge(path, existing, incoming) ?: incoming
            } else {
                incoming
            }
        }
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
