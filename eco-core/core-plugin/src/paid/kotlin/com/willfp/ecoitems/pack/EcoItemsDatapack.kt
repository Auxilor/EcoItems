package com.willfp.ecoitems.pack

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.willfp.eco.core.datapack.DatapackContributor
import com.willfp.eco.core.datapack.DatapackDraft
import com.willfp.eco.util.formatEco
import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.paintings.Painting
import com.willfp.ecoitems.paintings.Paintings
import com.willfp.ecoitems.sounds.JukeboxSong
import com.willfp.ecoitems.sounds.Sound
import com.willfp.ecoitems.sounds.Sounds
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import java.io.File
import java.util.Properties

/**
 * Contributes painting variants and jukebox songs to the EcoItems datapack.
 *
 * Both live in data-driven registries that only load at server start, so eco
 * writes the pack and logs a restart notice rather than applying them live.
 * Everything that isn't the JSON itself - paths, pack formats, ordering,
 * change detection, validation, cleanup - belongs to eco's datapack layer.
 *
 * The sorting below is redundant (eco canonicalises regardless) but keeps the
 * emitted order readable.
 */
class EcoItemsDatapack(
    private val plugin: EcoItemsPlugin
) : DatapackContributor {
    private val gson = GsonBuilder().disableHtmlEscaping().create()

    override fun contribute(draft: DatapackDraft) {
        // Paintings and jukebox songs are part of the pack; with it off, the
        // pack is emitted empty rather than holding content nothing serves.
        if (!EcoItemsPackFeature.isPackEnabled(plugin)) {
            return
        }

        for (painting in Paintings.values().sortedBy { it.id }) {
            val json = paintingJson(painting) ?: continue
            val key = key(painting.id) ?: continue

            draft.put("painting_variant", key, json)
        }

        for (sound in Sounds.values().sortedBy { it.id }) {
            val jukebox = sound.jukebox ?: continue
            val key = key(sound.id) ?: continue

            draft.put("jukebox_song", key, jukeboxJson(sound, jukebox))
        }
    }

    private fun paintingJson(painting: Painting): String? {
        val location = PackLocation.parse(painting.texture)
        if (location == null) {
            plugin.logger.warning("Skipping painting ${painting.id}: texture '${painting.texture}' is not a valid location")
            return null
        }

        val textureFile = plugin.dataFolder.resolve(
            "pack/assets/${location.namespace}/textures/painting/${location.path}.png"
        )
        if (!textureFile.exists() && location.namespace != "minecraft") {
            plugin.logger.warning(
                "Skipping painting ${painting.id}: texture file pack/assets/${location.namespace}/textures/painting/${location.path}.png does not exist"
            )
            return null
        }

        val json = JsonObject()
        json.addProperty("asset_id", location.key)
        json.addProperty("width", painting.width)
        json.addProperty("height", painting.height)
        painting.title?.let { json.add("title", text(it)) }
        painting.author?.let { json.add("author", text(it)) }

        return gson.toJson(json)
    }

    private fun jukeboxJson(sound: Sound, jukebox: JukeboxSong): String {
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

        return gson.toJson(json)
    }

    private fun key(id: String): NamespacedKey? =
        runCatching { plugin.namespacedKeyFactory.create(id) }.getOrElse {
            plugin.logger.warning("Skipping $id: not a valid registry ID (use lowercase letters, digits, _, - and .)")
            null
        }

    private fun text(value: String): JsonObject = JsonObject().apply {
        addProperty("text", value.formatEco())
    }
}

/**
 * The pre-eco datapack, written straight into `datapacks/ecoitems`.
 *
 * Eco writes to `datapacks/eco_ecoitems` instead, so the old directory has to
 * go: left in place it stays enabled and defines the same painting variant and
 * jukebox song IDs, and pack order silently decides which wins.
 */
object LegacyDatapack {
    private const val NAME = "ecoitems"

    /**
     * Delete the legacy pack, if it's there. Idempotent.
     */
    fun remove(plugin: EcoItemsPlugin) {
        val datapack = levelFolder().resolve("datapacks/$NAME")

        // Only ours: a pack that doesn't hold ecoitems data isn't one we wrote.
        if (!datapack.resolve("data/ecoitems").isDirectory) {
            return
        }

        if (datapack.deleteRecursively()) {
            plugin.logger.info("Removed the legacy EcoItems datapack; its content now lives in eco_ecoitems")
        } else {
            plugin.logger.warning(
                "Could not remove the legacy EcoItems datapack at ${datapack.path}; " +
                    "delete it by hand, or it will define the same IDs as eco_ecoitems"
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
}
