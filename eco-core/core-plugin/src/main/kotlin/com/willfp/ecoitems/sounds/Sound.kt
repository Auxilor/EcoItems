package com.willfp.ecoitems.sounds

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.registry.KRegistrable
import com.willfp.ecoitems.plugin
import java.util.Objects

// The categories the client accepts in sounds.json.
private val CATEGORIES = setOf(
    "master", "music", "record", "weather", "block",
    "hostile", "neutral", "player", "ambient", "voice"
)

class Sound(
    override val id: String,
    val config: Config
) : KRegistrable {
    /** The sound event key, e.g. for /playsound or the play_sound effect. */
    val key = "ecoitems:$id"

    val category: String? = config.getStringOrNull("category")
        ?.lowercase()
        // Bukkit calls it RECORDS; the client calls it record.
        ?.let { if (it == "records") "record" else it }
        ?.also {
            if (it !in CATEGORIES) {
                plugin.logger.warning("Sound $id has unknown category '$it', using master instead")
            }
        }
        ?.takeIf { it in CATEGORIES }

    val subtitle: String? = config.getStringOrNull("subtitle")

    /** When set, this sound is registered as a jukebox song (needs a restart). */
    val jukebox: JukeboxSong? = if (config.has("jukebox")) {
        JukeboxSong(this, config.getSubsection("jukebox"))
    } else {
        null
    }

    val entries: List<SoundEntry> = when {
        // A list of sections with per-entry options...
        config.getSubsections("sounds").isNotEmpty() ->
            config.getSubsections("sounds").map { SoundEntry(it.getString("sound"), it) }

        // ...or a plain list of file names...
        config.getStrings("sounds").isNotEmpty() ->
            config.getStrings("sounds").map { SoundEntry(it, null) }

        // ...or a single file name.
        else -> listOfNotNull(config.getStringOrNull("sound")?.let { SoundEntry(it, null) })
    }

    init {
        if (entries.isEmpty()) {
            plugin.logger.warning("Sound $id has no sound files; set sound or sounds in its config")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Sound) {
            return false
        }

        return this.id == other.id
    }

    override fun hashCode(): Int {
        return Objects.hash(this.id)
    }

    override fun toString(): String {
        return "Sound{$id}"
    }
}

/** Jukebox song registration for a sound - registered via the generated datapack. */
class JukeboxSong(sound: Sound, config: Config) {
    /** Shown in the disc tooltip and the jukebox now-playing popup. */
    val description: String = config.getStringOrNull("description")
        ?: sound.subtitle
        ?: "Music Disc"

    val lengthSeconds = config.getDoubleOrNull("length-seconds") ?: 120.0

    val comparatorOutput = (config.getIntOrNull("comparator-output") ?: 15).coerceIn(1, 15)

    /** (Optional) How far the song is audible, in blocks. */
    val range: Double? = config.getDoubleOrNull("range")
}

/** One file in a sound event, with the vanilla sounds.json options. */
class SoundEntry(name: String, config: Config?) {
    /** A [ns:]path location relative to sounds/, without extension. */
    val name: String = name.removeSuffix(".ogg").replace('\\', '/')

    val volume = config?.getDoubleOrNull("volume") ?: 1.0
    val pitch = config?.getDoubleOrNull("pitch") ?: 1.0
    val weight = config?.getIntOrNull("weight") ?: 1
    val stream = config?.getBoolOrNull("stream") ?: false
    val attenuationDistance = config?.getIntOrNull("attenuation-distance") ?: 16
    val preload = config?.getBoolOrNull("preload") ?: false

    /** Default entries collapse to the compact string form in sounds.json. */
    val isDefault: Boolean
        get() = volume == 1.0 && pitch == 1.0 && weight == 1 &&
            !stream && attenuationDistance == 16 && !preload
}
