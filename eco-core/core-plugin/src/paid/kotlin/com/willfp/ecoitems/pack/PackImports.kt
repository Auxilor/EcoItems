package com.willfp.ecoitems.pack

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.willfp.ecoitems.EcoItemsPlugin
import java.io.File
import java.util.zip.ZipFile

/**
 * Merges external resource packs (from other plugins like MythicMobs or
 * CustomNameplates, or hand-made packs) into the generated pack.
 *
 * Drop .zip files or folders into pack/imports/. They load lowest-priority
 * and in name order, so EcoItems' own assets and later imports win on
 * collision - except font, sounds.json, atlas, and lang files, which are
 * merged instead of replaced. Overlay entries from imported pack.mcmeta
 * files carry over into the final pack.mcmeta.
 */
class ImportedPacks(
    val entries: Map<String, ByteArray>,
    val overlays: List<JsonObject>
) {
    /**
     * Every codepoint the imported fonts define, so glyph auto-assignment
     * can route around them instead of colliding (imported Oraxen packs use
     * the same private-use ranges we do).
     */
    val fontCodepoints: Set<Int> by lazy {
        val codepoints = mutableSetOf<Int>()

        for ((path, bytes) in entries) {
            if (!path.matches(Regex("assets/[^/]+/font/.+\\.json"))) {
                continue
            }

            val providers = runCatching {
                JsonParser.parseString(bytes.decodeToString()).asJsonObject.getAsJsonArray("providers")
            }.getOrNull() ?: continue

            for (provider in providers) {
                val obj = provider.asJsonObject

                obj.getAsJsonArray("chars")?.forEach { row ->
                    row.asString.codePoints().forEach { if (it != 0) codepoints.add(it) }
                }
                obj.getAsJsonObject("advances")?.keySet()?.forEach { key ->
                    key.codePoints().forEach { codepoints.add(it) }
                }
            }
        }

        codepoints
    }

    companion object {
        val EMPTY = ImportedPacks(emptyMap(), emptyList())
    }
}

object PackImports {
    private val JUNK_FILES = setOf(".ds_store", "thumbs.db", "desktop.ini")

    /** Reads and merges everything in pack/imports/. */
    fun load(plugin: EcoItemsPlugin): ImportedPacks {
        val importsDir = plugin.dataFolder.resolve("pack/imports")

        val packs = importsDir.listFiles { file ->
            file.isDirectory || file.extension.equals("zip", ignoreCase = true)
        }?.sortedBy { it.name.lowercase() } ?: return ImportedPacks.EMPTY

        val entries = mutableMapOf<String, ByteArray>()
        val overlays = mutableListOf<JsonObject>()
        val origins = mutableMapOf<String, String>()

        for (pack in packs) {
            try {
                val files = readPack(pack)
                if (files.isEmpty()) {
                    plugin.logger.warning("Imported pack ${pack.name} has no assets folder or pack.mcmeta; skipping it")
                    continue
                }

                var added = 0
                for ((path, bytes) in files) {
                    if (path == "pack.mcmeta") {
                        overlays.addAll(overlayEntries(plugin, pack.name, bytes))
                        continue
                    }

                    val existing = entries[path]
                    if (existing != null) {
                        val merged = merge(path, existing, bytes)
                        if (merged != null) {
                            plugin.logger.info("Merged $path from ${pack.name} with ${origins[path]}")
                            entries[path] = merged
                            continue
                        }
                        plugin.logger.warning("${pack.name} overrides $path from ${origins[path]}")
                    }

                    entries[path] = bytes
                    origins[path] = pack.name
                    added++
                }

                plugin.logger.info("Imported $added files from pack ${pack.name}")
            } catch (e: Exception) {
                plugin.logger.warning("Failed to import pack ${pack.name}: $e")
            }
        }

        return ImportedPacks(entries, overlays)
    }

    /** All pack files as normalized path -> bytes, roots stripped. */
    private fun readPack(pack: File): Map<String, ByteArray> {
        val raw = mutableMapOf<String, ByteArray>()

        if (pack.isDirectory) {
            for (file in pack.walkTopDown().filter { it.isFile }) {
                raw[file.relativeTo(pack).invariantSeparatorsPath] = file.readBytes()
            }
        } else {
            ZipFile(pack).use { zip ->
                for (entry in zip.entries()) {
                    if (!entry.isDirectory) {
                        raw[entry.name] = zip.getInputStream(entry).use { it.readBytes() }
                    }
                }
            }
        }

        val roots = detectRoots(raw.keys)
        val files = mutableMapOf<String, ByteArray>()

        for ((path, bytes) in raw) {
            if (isJunk(path)) {
                continue
            }

            val normalized = normalize(path, roots) ?: continue
            files[normalized] = bytes
        }

        return files
    }

    /**
     * A pack root is whatever precedes an assets/ folder or contains a
     * pack.mcmeta - packs are often zipped inside a wrapper folder, and some
     * zips carry several packs.
     */
    private fun detectRoots(paths: Collection<String>): List<String> {
        val roots = sortedSetOf<String>()

        for (path in paths) {
            if (isJunk(path)) {
                continue
            }

            val assetsIndex = assetsFolderIndex(path)
            if (assetsIndex >= 0) {
                roots.add(path.substring(0, assetsIndex))
            }

            if (path.substringAfterLast('/') == "pack.mcmeta") {
                roots.add(path.removeSuffix("pack.mcmeta"))
            }
        }

        // Drop roots nested inside other roots.
        return roots.filter { root ->
            roots.none { other -> other != root && root.startsWith(other) }
        }
    }

    private fun assetsFolderIndex(path: String): Int {
        var index = 0
        while (true) {
            val found = path.indexOf("assets/", index)
            if (found < 0) {
                return -1
            }
            if (found == 0 || path[found - 1] == '/') {
                return found
            }
            index = found + 1
        }
    }

    private fun normalize(path: String, roots: List<String>): String? {
        for (root in roots) {
            if (!path.startsWith(root)) {
                continue
            }

            val relative = path.substring(root.length)

            // Keep base assets, overlay-directory assets, and root files.
            val isOverlayAssets = relative.indexOf("/assets/").let { it > 0 && '/' !in relative.substring(0, it) }
            if (relative.startsWith("assets/") || isOverlayAssets ||
                relative == "pack.mcmeta" || relative == "pack.png"
            ) {
                return relative
            }
        }

        return null
    }

    private fun isJunk(path: String): Boolean {
        if (path.substringAfterLast('/').lowercase() in JUNK_FILES) {
            return true
        }
        return path.startsWith("__MACOSX/") || "/__MACOSX/" in path
    }

    /**
     * Collision handling: fonts, sounds, atlases, and lang files merge with
     * the incoming file winning; anything else is null (replace + warn).
     * Also used when the pack/assets overlay lands on an imported file.
     */
    internal fun merge(path: String, existing: ByteArray, incoming: ByteArray): ByteArray? {
        return when {
            path.matches(Regex("assets/[^/]+/font/.+\\.json")) ->
                mergeJson(existing, incoming) { old, new ->
                    // Later imports win: the first provider defining a
                    // character is used, so the new pack's providers go first.
                    val providers = JsonArray()
                    new.getAsJsonArray("providers")?.forEach { providers.add(it) }
                    old.getAsJsonArray("providers")?.forEach { providers.add(it) }
                    JsonObject().apply { add("providers", providers) }
                }

            path.matches(Regex("assets/[^/]+/sounds\\.json")) ||
                path.matches(Regex("assets/[^/]+/lang/.+\\.json")) ->
                mergeJson(existing, incoming) { old, new ->
                    // Later imports win per key.
                    for ((key, value) in new.entrySet()) {
                        old.add(key, value)
                    }
                    old
                }

            path.matches(Regex("assets/[^/]+/atlases/.+\\.json")) ->
                mergeJson(existing, incoming) { old, new ->
                    val sources = JsonArray()
                    old.getAsJsonArray("sources")?.forEach { sources.add(it) }
                    new.getAsJsonArray("sources")?.forEach { sources.add(it) }
                    JsonObject().apply { add("sources", sources) }
                }

            else -> null
        }
    }

    private fun mergeJson(
        existing: ByteArray,
        incoming: ByteArray,
        merger: (JsonObject, JsonObject) -> JsonObject
    ): ByteArray? {
        val old = runCatching { JsonParser.parseString(existing.decodeToString()).asJsonObject }.getOrNull()
        val new = runCatching { JsonParser.parseString(incoming.decodeToString()).asJsonObject }.getOrNull()

        if (old == null || new == null) {
            return null
        }

        return merger(old, new).toString().encodeToByteArray()
    }

    internal fun overlayEntries(plugin: EcoItemsPlugin, packName: String, mcmeta: ByteArray): List<JsonObject> {
        return runCatching {
            JsonParser.parseString(mcmeta.decodeToString()).asJsonObject
                .getAsJsonObject("overlays")
                ?.getAsJsonArray("entries")
                ?.map { it.asJsonObject }
                ?: emptyList()
        }.getOrElse {
            plugin.logger.warning("Could not read overlay entries from $packName's pack.mcmeta")
            emptyList()
        }
    }
}
