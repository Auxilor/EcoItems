package com.willfp.ecoitems.pack

import com.willfp.ecoitems.EcoItemsPlugin
import java.io.File
import java.util.zip.ZipFile

/**
 * Sets up the editable /pack folder: extracts the bundled default assets on
 * first run (or after the folder is deleted), and makes sure the folders
 * users put their own resources in always exist.
 */
object PackDefaults {
    fun ensure(plugin: EcoItemsPlugin) {
        val packDir = plugin.dataFolder.resolve("pack")

        if (!packDir.exists()) {
            extract(plugin)
        } else {
            // Upgrade paths: installs from before a feature existed get its
            // shipped defaults.
            for (feature in listOf("glyphs", "sounds", "lang")) {
                if (!packDir.resolve(feature).exists()) {
                    extract(plugin, only = "pack/$feature/")
                }
            }

            extract(plugin, only = "pack/glyphs/gui/", overwrite = false)
            extract(plugin, only = "pack/assets/ecoitems/", overwrite = false)
        }

        for (directory in listOf("textures", "models", "assets", "glyphs", "sounds", "lang")) {
            packDir.resolve(directory).mkdirs()
        }
    }

    private fun extract(plugin: EcoItemsPlugin, only: String? = null, overwrite: Boolean = true) {
        val dataPath = plugin.dataFolder.toPath().normalize()

        try {
            ZipFile(plugin.jar).use { zip ->
                for (entry in zip.entries()) {
                    if (entry.isDirectory ||
                        !entry.name.startsWith(only ?: "pack/") ||
                        entry.name.startsWith("pack/defaults/")
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
