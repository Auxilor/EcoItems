package com.willfp.ecoitems.pack

import com.willfp.ecoitems.EcoItemsPlugin
import java.io.File
import java.util.zip.ZipFile

/**
 * Sets up the editable /pack folder: extracts the bundled default assets on
 * first run (or after the folder is deleted), and makes sure the folders
 * users put their own resources in always exist.
 *
 * The pack folder mirrors a vanilla resource pack (assets/<namespace>/...),
 * plus pack/imports/ for merging external packs.
 */
object PackDefaults {
    // The pre-release hybrid layout; detected to help early adopters migrate.
    private val LEGACY_DIRECTORIES = listOf("textures", "models", "glyphs", "sounds", "lang")

    fun ensure(plugin: EcoItemsPlugin) {
        val packDir = plugin.dataFolder.resolve("pack")

        if (!packDir.exists()) {
            extract(plugin)
        } else {
            // Ship any new bundled defaults without touching edited files.
            extract(plugin, only = "pack/assets/ecoitems/", overwrite = false)
        }

        packDir.resolve("assets").mkdirs()
        packDir.resolve("imports").mkdirs()

        for (legacy in LEGACY_DIRECTORIES) {
            if (packDir.resolve(legacy).exists()) {
                plugin.logger.warning(
                    "The pack layout changed: pack/$legacy/ is no longer used. The pack folder now mirrors " +
                        "a vanilla resource pack - move your files into pack/assets/<namespace>/ " +
                        "(see the resource pack documentation) and delete pack/$legacy/."
                )
            }
        }
    }

    private fun extract(plugin: EcoItemsPlugin, only: String? = null, overwrite: Boolean = true) {
        val dataPath = plugin.dataFolder.toPath().normalize()

        try {
            ZipFile(plugin.jar).use { zip ->
                for (entry in zip.entries()) {
                    if (entry.isDirectory ||
                        !entry.name.startsWith(only ?: "pack/") ||
                        entry.name.startsWith("pack/defaults/") ||
                        entry.name.startsWith("pack/builtin/")
                    ) {
                        continue
                    }

                    val target = File(plugin.dataFolder, entry.name)
                    if (!target.toPath().normalize().startsWith(dataPath)) {
                        continue
                    }
                    if (!overwrite && target.exists()) {
                        continue
                    }

                    target.parentFile.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        target.outputStream().use { input.copyTo(it) }
                    }
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to extract default pack assets: $e")
        }
    }
}
