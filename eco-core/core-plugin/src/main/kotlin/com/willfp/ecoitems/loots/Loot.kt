package com.willfp.ecoitems.loots

import com.willfp.eco.core.blocks.Blocks
import com.willfp.eco.core.blocks.TestableBlock
import com.willfp.eco.core.blocks.impl.EmptyTestableBlock
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.entities.Entities
import com.willfp.eco.core.entities.TestableEntity
import com.willfp.eco.core.entities.impl.EmptyTestableEntity
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.TestableItem
import com.willfp.eco.core.recipe.parts.EmptyTestableItem
import com.willfp.eco.core.registry.KRegistrable
import com.willfp.ecoitems.blocks.parseIntRange
import com.willfp.ecoitems.plugin
import com.willfp.libreforge.EmptyProvidedHolder
import com.willfp.libreforge.ViolationContext
import com.willfp.libreforge.conditions.Conditions
import com.willfp.libreforge.toDispatcher
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Biome
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
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

    /** Block lookup strings or entity types this loot drops from (fishing ignores it). */
    val targets = config.getStrings("targets")
        .map { it.lowercase().removePrefix("minecraft:") }
        .toSet()

    // Only fully resolved target lists are cached: a plugin registering its
    // blocks or entities after the first roll should start matching, not stay
    // broken until the next reload. Each bad key warns once.
    private val warnedTargets = mutableSetOf<String>()
    private var cachedTargetBlocks: List<TestableBlock>? = null
    private var cachedTargetEntities: List<TestableEntity>? = null

    private fun targetBlocks(): List<TestableBlock> {
        cachedTargetBlocks?.let { return it }

        val compiled = targets.mapNotNull {
            Blocks.lookup(it).takeUnless { found -> found is EmptyTestableBlock }
                ?: warnUnknownTarget(it, "block")
        }

        return compiled.also {
            if (it.size == targets.size) {
                cachedTargetBlocks = it
            }
        }
    }

    private fun targetEntities(): List<TestableEntity> {
        cachedTargetEntities?.let { return it }

        val compiled = targets.mapNotNull {
            Entities.lookup(it).takeUnless { found -> found is EmptyTestableEntity }
                ?: warnUnknownTarget(it, "entity")
        }

        return compiled.also {
            if (it.size == targets.size) {
                cachedTargetEntities = it
            }
        }
    }

    private fun <T> warnUnknownTarget(target: String, kind: String): T? {
        if (warnedTargets.add(target)) {
            plugin.logger.warning("Loot $id has unknown $kind target '$target'")
        }
        return null
    }

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
                id,
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

    /** Whether this loot rolls successfully for one broken block. */
    fun rollsForBlock(block: Block, player: Player): Boolean {
        if (type != LootType.BLOCK || !Blocks.matchesAny(block, targetBlocks())) {
            return false
        }
        return rolls(block.world, block.biome, player)
    }

    /** Whether this loot rolls successfully for one killed entity. */
    fun rollsForEntity(entity: Entity, player: Player): Boolean {
        if (type != LootType.MOB || targetEntities().none { it.matches(entity) }) {
            return false
        }
        val location = entity.location
        return rolls(location.world ?: return false, location.block.biome, player)
    }

    /** Whether this loot rolls successfully for one catch. */
    fun rollsForFishing(world: World, biome: Biome, player: Player): Boolean {
        if (type != LootType.FISHING) {
            return false
        }
        return rolls(world, biome, player)
    }

    private fun rolls(world: World, biome: Biome, player: Player): Boolean {
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
    val lootId: String,
    val item: String,
    val chance: Double,
    val amount: IntRange
) {
    private var cached: TestableItem? = null
    private var warned = false

    /** A fresh stack of this drop, or null if the lookup is invalid. */
    fun toItemStack(): ItemStack? {
        val testable = cached ?: Items.lookup(item)
            .takeUnless { it is EmptyTestableItem }
            ?.also { cached = it }

        if (testable == null) {
            if (!warned) {
                warned = true
                plugin.logger.warning("Loot $lootId drop '$item' is not a valid item")
            }
            return null
        }

        return testable.item.takeIf { it.type != Material.AIR }
    }
}
