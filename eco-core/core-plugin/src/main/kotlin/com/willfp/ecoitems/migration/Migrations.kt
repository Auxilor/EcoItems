package com.willfp.ecoitems.migration

import com.willfp.ecoitems.EcoItemsPlugin
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

/**
 * Imports configs and pack assets from other custom item plugins. The user
 * copies the source plugin's folder contents into migrations/<Source>/ and
 * runs /ecoitems migrate <source>.
 */
object Migrations {
    val SOURCES = listOf("Oraxen", "Nexo", "ItemsAdder")

    fun ensureFolders(plugin: EcoItemsPlugin) {
        val root = plugin.dataFolder.resolve("migrations")
        for (source in SOURCES) {
            root.resolve(source).mkdirs()
        }

        val readme = root.resolve("README.txt")
        if (!readme.exists()) {
            readme.writeText(
                """
                Migrating from another custom items plugin:

                1. Copy the contents of the old plugin's folder into the matching
                   subfolder here (e.g. everything inside plugins/Oraxen/ into
                   migrations/Oraxen/).
                2. Back up your worlds.
                3. Run /ecoitems migrate <oraxen|nexo|itemsadder> from the console.
                4. Check the console for anything that could not be converted.

                Converted configs are written to items/imported/, glyphs/imported/
                and sounds/imported/; pack assets are copied into pack/. Nothing in
                this folder is ever loaded directly, and running a migration twice
                skips files that already exist.
                """.trimIndent() + "\n"
            )
        }

        for (source in SOURCES) {
            val folder = root.resolve(source)
            if (folder.listFiles()?.isNotEmpty() == true) {
                plugin.logger.info("Found files in migrations/$source - run /ecoitems migrate ${source.lowercase()} to import them")
            }
        }
    }

    /** Runs a migration; null if the source name is unknown. */
    fun migrate(plugin: EcoItemsPlugin, source: String): MigrationResult? {
        val folder = SOURCES.firstOrNull { it.equals(source, ignoreCase = true) } ?: return null

        plugin.logger.info("Starting migration from $folder...")
        val result = when (folder) {
            "Oraxen" -> OraxenLikeMigration(plugin, Dialect.ORAXEN).run()
            "Nexo" -> OraxenLikeMigration(plugin, Dialect.NEXO).run()
            else -> ItemsAdderMigration(plugin).run()
        }
        plugin.logger.info("Finished migration from $folder: ${result.summary()}")

        return result
    }
}

class MigrationResult(private val plugin: EcoItemsPlugin) {
    var items = 0
    var blocks = 0
    var furniture = 0
    var glyphs = 0
    var sounds = 0
    var recipes = 0
    var assets = 0
    var warnings = 0

    fun warn(message: String) {
        warnings++
        plugin.logger.warning("[Migration] $message")
    }

    fun summary(): String =
        "$items items ($blocks blocks, $furniture furniture), $glyphs glyphs, $sounds sounds, " +
            "$recipes recipes, $assets pack files" +
            if (warnings > 0) "; $warnings warnings (see console)" else ""
}

/** Writes a config to a file, skipping (and warning) if it already exists. */
internal fun writeConverted(result: MigrationResult, file: File, config: YamlConfiguration): Boolean {
    if (file.exists()) {
        result.warn("Skipping ${file.name}: already exists (delete it to re-import)")
        return false
    }

    file.parentFile.mkdirs()
    file.writeText(config.saveToString())
    return true
}

/** Copies a tree, skipping junk and files that already exist. Returns files copied. */
internal fun copyTree(from: File, to: File): Int {
    if (!from.isDirectory) return 0

    var copied = 0
    for (file in from.walkTopDown().filter { it.isFile && !isJunk(it, from) }) {
        val target = to.resolve(file.relativeTo(from).invariantSeparatorsPath)
        if (target.exists()) continue
        target.parentFile.mkdirs()
        file.copyTo(target)
        copied++
    }
    return copied
}

/** macOS/Windows filesystem litter that rides along in zips. */
internal fun isJunk(file: File, root: File): Boolean =
    file.name in setOf(".DS_Store", "Thumbs.db", "desktop.ini") ||
        file.name.startsWith("._") ||
        file.relativeTo(root).invariantSeparatorsPath.contains("__MACOSX")

/**
 * Users sometimes extract archives with a wrapping folder (migrations/Nexo/
 * Nexo/items/...) - descend one level if the markers only exist there.
 */
internal fun resolveRoot(plugin: EcoItemsPlugin, folder: File, vararg markers: String): File {
    if (markers.any { folder.resolve(it).exists() }) {
        return folder
    }

    val nested = folder.listFiles()
        ?.filter { it.isDirectory && it.name != "__MACOSX" }
        ?.singleOrNull { candidate -> markers.any { candidate.resolve(it).exists() } }

    if (nested != null) {
        plugin.logger.info("Using nested folder migrations/${folder.name}/${nested.name} as the migration root")
        return nested
    }

    return folder
}
