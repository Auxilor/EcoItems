package com.willfp.ecoitems.crops

import com.willfp.eco.core.config.config
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecoitems.blocks.BlockDrops
import com.willfp.ecoitems.blocks.EcoBlock
import com.willfp.ecoitems.plugin

/**
 * The crop half of an item config's `crop:` section. The item is the seed;
 * the crop in the world is a stringblock-backed custom block with one state
 * per growth stage, advanced on a timer by [CropTracker].
 */
class EcoCrop(val id: String, val config: Config) {
    /** One subsection per growth stage, lowest first; each has texture/model. */
    val stages: List<Config> = config.getSubsections("stages")

    /** Average seconds from planting to fully grown. */
    val growthTime = config.getIntOrNull("growth-time") ?: 1200

    val requiresFarmland = config.getBoolOrNull("requires-farmland") ?: true

    val minLight = (config.getIntOrNull("min-light") ?: 9).coerceIn(0, 15)

    val bonemeal = config.getBoolOrNull("bonemeal") ?: true

    /** Growth speed multipliers by weather (above 1 = faster). */
    val rainMultiplier = config.getDoubleOrNull("rain-multiplier") ?: 1.0
    val thunderMultiplier = config.getDoubleOrNull("thunder-multiplier") ?: 1.0
    val snowMultiplier = config.getDoubleOrNull("snow-multiplier") ?: 1.0

    /** Drops when broken fully grown; null = the seed item. */
    val drops = if (config.has("drops")) BlockDrops(id, config.getSubsection("drops")) else null

    /** Drops when broken early; null = the seed item. */
    val immatureDrops = if (config.has("immature-drops")) {
        BlockDrops(id, config.getSubsection("immature-drops"))
    } else {
        null
    }

    val perStageSeconds: Int
        get() = (growthTime / (stages.size - 1).coerceAtLeast(1)).coerceAtLeast(1)

    /**
     * The world presence: a stringblock with one state per stage, reusing
     * the stackable per-orientation model machinery for the pack build.
     */
    val block = run {
        val models = stages.mapNotNull { it.getStringOrNull("model") }
        val textures = stages.mapNotNull { it.getStringOrNull("texture") }

        val (refKey, refs) = when {
            stages.isNotEmpty() && models.size == stages.size -> "models" to models
            stages.isNotEmpty() && textures.size == stages.size -> "textures" to textures
            else -> {
                if (stages.isNotEmpty()) {
                    plugin.logger.warning("Crop $id: every stage needs a texture (or every stage a model)")
                }
                "textures" to textures
            }
        }

        EcoBlock(
            id,
            config {
                "type" to "stringblock"
                "stackable" to config {
                    refKey to refs
                }
                "sounds" to config {
                    "place" to "minecraft:item.crop.plant"
                    "break" to "minecraft:block.crop.break"
                    "volume" to 1.0
                    "pitch" to 1.0
                }
            },
            crop = this
        )
    }

    init {
        if (stages.size < 2) {
            plugin.logger.warning("Crop $id needs at least two stages")
        }
    }
}
