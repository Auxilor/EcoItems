package com.willfp.ecoitems.pack

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.sounds.Sound

/**
 * Generates assets/ecoitems/sounds.json from the sound configs. Sound events
 * play as ecoitems:<id>; ogg files live in pack/sounds/ and are mapped into
 * the pack wholesale by [PackBuilder].
 *
 * Runs after the pack/assets overlay copy so a user-supplied sounds.json is
 * merged rather than clobbered (user entries win on collision).
 */
object SoundAssetGenerator {
    private const val SOUNDS_JSON = "assets/ecoitems/sounds.json"
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    fun generate(
        plugin: EcoItemsPlugin,
        sounds: Collection<Sound>,
        entries: MutableMap<String, ByteArray>
    ) {
        if (sounds.isEmpty() && SOUNDS_JSON !in entries) {
            return
        }

        val json = JsonObject()

        for (sound in sounds.sortedBy { it.id }) {
            val event = JsonObject()

            sound.category?.let { event.addProperty("category", it) }
            sound.subtitle?.let { event.addProperty("subtitle", it) }

            val files = JsonArray()
            for (entry in sound.entries) {
                val name = resolveName(plugin, sound, entry.name) ?: continue

                if (entry.isDefault) {
                    files.add(name)
                    continue
                }

                val file = JsonObject()
                file.addProperty("name", name)
                if (entry.volume != 1.0) file.addProperty("volume", entry.volume)
                if (entry.pitch != 1.0) file.addProperty("pitch", entry.pitch)
                if (entry.weight != 1) file.addProperty("weight", entry.weight)
                if (entry.stream) file.addProperty("stream", true)
                if (entry.attenuationDistance != 16) file.addProperty("attenuation_distance", entry.attenuationDistance)
                if (entry.preload) file.addProperty("preload", true)
                files.add(file)
            }

            if (files.isEmpty()) {
                continue
            }

            event.add("sounds", files)
            json.add(sound.id, event)
        }

        // A user-supplied sounds.json from pack/assets wins on collision.
        entries[SOUNDS_JSON]?.let { existing ->
            runCatching { JsonParser.parseString(existing.decodeToString()).asJsonObject }
                .getOrNull()
                ?.entrySet()
                ?.forEach { (key, value) -> json.add(key, value) }
                ?: plugin.logger.warning("pack/assets/ecoitems/sounds.json is not a valid sounds file; replacing it")
        }

        if (json.size() > 0) {
            entries[SOUNDS_JSON] = gson.toJson(json).encodeToByteArray()
        }
    }

    /**
     * A bare name references pack/sounds/<name>.ogg (and must exist there);
     * a namespaced name (e.g. vanilla samples like minecraft:dig/stone1)
     * passes through verbatim.
     *
     * Bare names must be written fully qualified: the client resolves
     * unqualified names in sounds.json against the minecraft namespace,
     * not the namespace the sounds.json belongs to.
     */
    private fun resolveName(plugin: EcoItemsPlugin, sound: Sound, name: String): String? {
        if (":" in name) {
            return name
        }

        if (!plugin.dataFolder.resolve("pack/sounds/$name.ogg").exists()) {
            plugin.logger.warning(
                "Skipping file $name of sound ${sound.id}: pack/sounds/$name.ogg does not exist"
            )
            return null
        }

        return "ecoitems:$name"
    }
}
