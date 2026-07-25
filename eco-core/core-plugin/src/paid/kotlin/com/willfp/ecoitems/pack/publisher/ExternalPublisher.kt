package com.willfp.ecoitems.pack.publisher

import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.pack.BuiltPack
import java.io.File

/**
 * Exports the pack to a directory for the admin to host themselves (on a CDN,
 * web server, etc). The public URL of the exported zip goes in pack.yml.
 */
class ExternalPublisher(
    private val plugin: EcoItemsPlugin,
    private val directory: String,
    private val url: String
) : PackPublisher {
    override fun publish(pack: BuiltPack): PublishedPack? {
        val dir = File(directory).takeIf { it.isAbsolute } ?: plugin.dataFolder.resolve(directory)

        try {
            dir.mkdirs()
            pack.file.copyTo(dir.resolve("pack.zip"), overwrite = true)
        } catch (e: Exception) {
            plugin.logger.severe("Could not export pack to $dir: $e")
            return null
        }

        if (url.isBlank()) {
            plugin.logger.info("Exported pack to ${dir.resolve("pack.zip")} (sha1 ${pack.sha1}); set delivery.external.url to deliver it to players")
            return null
        }

        return PublishedPack(url, pack)
    }
}
