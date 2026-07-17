package com.willfp.ecoitems.blocks

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecoitems.libreforge.ContentEffects
import com.willfp.ecoitems.plugin

/**
 * The world-block half of an item config's `block:` section. The item is the
 * placer; this describes what exists in the world once placed.
 */
class EcoBlock(val id: String, val config: Config) {
    val backing = BlockBacking.parse(config.getString("type")) ?: run {
        plugin.logger.warning("Block $id has unknown type '${config.getString("type")}', using noteblock")
        BlockBacking.NOTEBLOCK
    }

    val directional: DirectionalType? = config.getStringOrNull("directional")?.let { value ->
        DirectionalType.entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: run {
            plugin.logger.warning("Block $id has unknown directional type '$value' (log, furnace, or dropper)")
            null
        }
    }.also {
        if (it != null && backing != BlockBacking.NOTEBLOCK) {
            plugin.logger.warning("Block $id: directional blocks must be noteblock-backed")
        }
    }

    /** Sea-pickle-style stacking: one state and model per stack count. */
    val stackable = if (config.has("stackable")) {
        StackableBlock(id, config.getSubsection("stackable"))
    } else {
        null
    }.also {
        if (it != null && backing != BlockBacking.STRINGBLOCK) {
            plugin.logger.warning("Block $id: stackable blocks must be stringblock-backed")
        }
        if (it != null && directional != null) {
            plugin.logger.warning("Block $id: stackable and directional don't combine; using directional")
        }
    }

    /**
     * Orientation keys; a single empty key for plain blocks. Stackable blocks
     * reuse the orientation slots as stack counts (index 0 = one item).
     */
    val orientations = when {
        directional != null && backing == BlockBacking.NOTEBLOCK -> directional.orientations
        directional == null && stackable != null && backing == BlockBacking.STRINGBLOCK && stackable.count > 0 ->
            List(stackable.count) { "stack${it + 1}" }
        else -> listOf("")
    }

    /** Explicit variation (for imported configs); auto-assigned when absent. */
    val configuredVariation = config.getIntOrNull("variation")

    /** The block id this block becomes when stripped with an axe. */
    val stripsTo = config.getStringOrNull("strips-to")

    val hardness = config.getDoubleOrNull("hardness") ?: -1.0

    val light = (config.getIntOrNull("light") ?: 0).coerceIn(0, 15)

    val falling = config.getBool("falling")

    val blastResistant = config.getBool("blast-resistant")

    /** Tool categories that mine this block at full speed (PICKAXE, AXE, ...). */
    val correctTools = config.getStrings("correct-tools").map { it.uppercase() }

    /** Minimum tool tier (wooden/stone/iron/golden/diamond/netherite) for drops. */
    val minimumTier = config.getStringOrNull("minimum-tier")?.lowercase()

    /** Whether the pack build has anything to generate for this block. */
    val hasAssets = config.has("texture") || config.has("textures") || config.has("model") ||
        stackable?.count?.let { it > 0 } == true

    /** null = drop the placer item itself. */
    val drops = if (config.has("drops")) BlockDrops(id, config.getSubsection("drops")) else null

    val sounds = if (config.has("sounds")) BlockSounds(config.getSubsection("sounds")) else null

    /** Effects run when players interact with the placed block. */
    val effects = ContentEffects("Block $id", config)
}

enum class DirectionalType(val orientations: List<String>) {
    LOG(listOf("y", "x", "z")),
    FURNACE(listOf("north", "east", "south", "west")),
    DROPPER(listOf("north", "east", "south", "west", "up", "down"))
}

class BlockDrops(blockId: String, config: Config) {
    /** Silk touch drops the block item itself instead of the loot. */
    val silkTouch = config.getBool("silk-touch")

    /** Fortune multiplies loot amounts. */
    val fortune = config.getBool("fortune")

    val xp = parseIntRange(config.getString("xp"))

    val items = config.getSubsections("items").mapNotNull { drop ->
        val item = drop.getStringOrNull("item")
        if (item == null) {
            plugin.logger.warning("Block $blockId has a drop with no item")
            null
        } else {
            BlockDropItem(
                item,
                drop.getDoubleOrNull("chance") ?: 1.0,
                parseIntRange(drop.getStringOrNull("amount") ?: "1")
            )
        }
    }
}

/** Parses "2" or "1-3" into a range; blank/invalid parses empty. */
internal fun parseIntRange(value: String): IntRange {
    if (value.isBlank()) return IntRange.EMPTY
    val parts = value.split("-", limit = 2).mapNotNull { it.trim().toIntOrNull() }
    return when (parts.size) {
        2 -> parts[0]..parts[1]
        1 -> parts[0]..parts[0]
        else -> IntRange.EMPTY
    }
}

class BlockDropItem(
    val item: String,
    val chance: Double,
    val amount: IntRange
)

class StackableBlock(blockId: String, config: Config) {
    /** Model references, one per stack count (or textures to generate from). */
    val models = config.getStrings("models")
    val textures = config.getStrings("textures")

    val count = if (models.isNotEmpty()) models.size else textures.size

    init {
        if (count == 0) {
            plugin.logger.warning("Block $blockId: stackable needs a models: or textures: list")
        }
    }
}

class BlockSounds(config: Config) {
    val place = config.getStringOrNull("place")
    val breakSound = config.getStringOrNull("break")
    val step = config.getStringOrNull("step")
    val hit = config.getStringOrNull("hit")
    val fall = config.getStringOrNull("fall")
    val volume = config.getDoubleOrNull("volume") ?: 1.0
    val pitch = config.getDoubleOrNull("pitch") ?: 0.8
}
