package com.willfp.ecoitems.loots

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.registry.KRegistrable
import com.willfp.ecoitems.blocks.parseIntRange
import com.willfp.ecoitems.plugin
import com.willfp.libreforge.EmptyProvidedHolder
import com.willfp.libreforge.ViolationContext
import com.willfp.libreforge.conditions.Conditions
import com.willfp.libreforge.toDispatcher
import org.bukkit.World
import org.bukkit.block.Biome
import org.bukkit.entity.Player
import java.util.Objects
import kotlin.random.Random

enum class LootType {
    BLOCK,
    MOB,
    FISHING;

    companion object {
        fun fromID(id: String): LootType? =
            entries.firstOrNull { it.name.equals(id, ignoreCase = true) }
    }
}

/**
 * Custom loot injected into vanilla gameplay: extra drops from breaking
 * vanilla blocks, killing mobs, or fishing.
 */
class Loot(
    override val id: String,
    val config: Config
) : KRegistrable {
    val type = LootType.fromID(config.getString("type")) ?: LootType.BLOCK.also {
        plugin.logger.warning("Loot $id has unknown type '${config.getString("type")}' (block, mob, or fishing)")
    }

    /** Block materials or entity types this loot drops from (fishing ignores it). */
    val targets = config.getStrings("targets")
        .map { it.lowercase().removePrefix("minecraft:") }
        .toSet()

    /** The chance for this loot to roll at all, 0-1. */
    val chance = config.getDoubleOrNull("chance") ?: 1.0

    /** Fortune on the tool multiplies amounts (block loot only). */
    val fortune = config.getBool("fortune")

    val xp = parseIntRange(config.getString("xp"))

    /** Optional biome/world filters (empty = everywhere). */
    val biomes = config.getStrings("biomes")
        .map { it.lowercase().removePrefix("minecraft:") }
        .toSet()

    val worlds = config.getStrings("worlds").map { it.lowercase() }.toSet()

    val items = config.getSubsections("items").mapNotNull { drop ->
        val item = drop.getStringOrNull("item")
        if (item == null) {
            plugin.logger.warning("Loot $id has a drop with no item")
            null
        } else {
            LootItem(
                item,
                drop.getDoubleOrNull("chance") ?: 1.0,
                parseIntRange(drop.getStringOrNull("amount") ?: "1")
            )
        }
    }

    val conditions = Conditions.compile(
        config.getSubsections("conditions"),
        ViolationContext(plugin, "Loot ID $id")
    )

    init {
        if (targets.isEmpty() && type != LootType.FISHING) {
            plugin.logger.warning("Loot $id has no targets, so it will never drop")
        }
        if (items.isEmpty()) {
            plugin.logger.warning("Loot $id has no items")
        }
    }

    /** Whether this loot rolls successfully for one break/kill/catch. */
    fun rolls(target: String?, world: World, biome: Biome, player: Player): Boolean {
        if (type != LootType.FISHING && (target == null || target !in targets)) {
            return false
        }
        if (worlds.isNotEmpty() && world.name.lowercase() !in worlds) {
            return false
        }
        if (biomes.isNotEmpty() && biome.key.key !in biomes) {
            return false
        }
        if (!conditions.areMet(player.toDispatcher(), EmptyProvidedHolder)) {
            return false
        }
        return Random.nextDouble() < chance
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Loot) {
            return false
        }

        return this.id == other.id
    }

    override fun hashCode(): Int {
        return Objects.hash(this.id)
    }

    override fun toString(): String {
        return "Loot{$id}"
    }
}

class LootItem(
    val item: String,
    val chance: Double,
    val amount: IntRange
)
