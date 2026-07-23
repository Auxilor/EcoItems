package com.willfp.ecoitems.pack

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.willfp.eco.util.formatEco
import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.paintings.Painting
import com.willfp.ecoitems.sounds.Sound
import org.bukkit.Bukkit
import java.io.File
import java.util.Properties

/**
 * Writes the EcoItems datapack into the main world: painting variants and
 * jukebox songs live in data-driven registries, which only load at server
 * start - so changes here log a restart notice rather than applying live.
 */
object DatapackGenerator {
    // Datapack formats (distinct from resource pack formats): 81 = 1.21.8,
    // 101 = 26.1. Bump MAX_FORMAT alongside PackMcmeta's.
    private const val MIN_FORMAT = 81
    private const val MAX_FORMAT = 102

    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    fun write(plugin: EcoItemsPlugin, paintings: Collection<Painting>, sounds: Collection<Sound>) {
        val datapack = levelFolder().resolve("datapacks/ecoitems")

        val files = sortedMapOf<String, String>()

        for (painting in paintings.sortedBy { it.id }) {
            val location = PackLocation.parse(painting.texture)
            if (location == null) {
                plugin.logger.warning("Skipping painting ${painting.id}: texture '${painting.texture}' is not a valid location")
                continue
            }

            val textureFile = plugin.dataFolder.resolve(
                "pack/assets/${location.namespace}/textures/painting/${location.path}.png"
            )
            if (!textureFile.exists() && location.namespace != "minecraft") {
                plugin.logger.warning(
                    "Skipping painting ${painting.id}: texture file pack/assets/${location.namespace}/textures/painting/${location.path}.png does not exist"
                )
                continue
            }

            val json = JsonObject()
            json.addProperty("asset_id", location.key)
            json.addProperty("width", painting.width)
            json.addProperty("height", painting.height)
            painting.title?.let { json.add("title", text(it)) }
            painting.author?.let { json.add("author", text(it)) }

            files["data/ecoitems/painting_variant/${painting.id}.json"] = gson.toJson(json)
        }

        for (sound in sounds.sortedBy { it.id }) {
            val jukebox = sound.jukebox ?: continue

            val json = JsonObject()
            // Always the inline form: a plain string would reference the
            // server's sound_event registry, which doesn't know pack sounds.
            val event = JsonObject()
            event.addProperty("sound_id", sound.key)
            jukebox.range?.let { event.addProperty("range", it) }
            json.add("sound_event", event)
            json.add("description", text(jukebox.description))
            json.addProperty("length_in_seconds", jukebox.lengthSeconds)
            json.addProperty("comparator_output", jukebox.comparatorOutput)

            files["data/ecoitems/jukebox_song/${sound.id}.json"] = gson.toJson(json)
        }

        if (files.isEmpty()) {
            if (datapack.exists()) {
                datapack.deleteRecursively()
                plugin.logger.info("Removed the EcoItems datapack; restart to unregister its content")
            }
            return
        }

        files["pack.mcmeta"] = """
            {
              "pack": {
                "description": "EcoItems paintings and jukebox songs",
                "pack_format": $MIN_FORMAT,
                "supported_formats": { "min_inclusive": $MIN_FORMAT, "max_inclusive": $MAX_FORMAT },
                "min_format": $MIN_FORMAT,
                "max_format": $MAX_FORMAT
              }
            }
        """.trimIndent()

        var changed = false

        for ((path, content) in files) {
            val target = datapack.resolve(path)
            if (target.exists() && target.readText() == content) {
                continue
            }
            target.parentFile.mkdirs()
            target.writeText(content)
            changed = true
        }

        // Registrations removed from config.
        for (registry in listOf("painting_variant", "jukebox_song")) {
            datapack.resolve("data/ecoitems/$registry").listFiles()?.forEach { file ->
                if ("data/ecoitems/$registry/${file.name}" !in files) {
                    file.delete()
                    changed = true
                }
            }
        }

        if (changed) {
            plugin.logger.info(
                "Updated the EcoItems datapack (paintings and jukebox songs); restart the server to register the changes"
            )
        }
    }

    /**
     * The server only scans `<world container>/<level-name>/datapacks`.
     * Resolving through Bukkit.getWorlds() is wrong on setups where the first
     * world's folder is dimension-nested (world/dimensions/minecraft/overworld).
     */
    private fun levelFolder(): File {
        val properties = Properties()
        val serverProperties = File("server.properties")
        if (serverProperties.exists()) {
            serverProperties.inputStream().use(properties::load)
        }

        return File(Bukkit.getWorldContainer(), properties.getProperty("level-name", "world"))
    }

    private fun text(value: String): JsonObject = JsonObject().apply {
        addProperty("text", value.formatEco())
    }
}
