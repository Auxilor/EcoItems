package com.willfp.ecoitems.blocks

import com.willfp.eco.core.blocks.Blocks
import com.willfp.eco.core.blocks.CustomBlock
import com.willfp.eco.core.blocks.TestableBlock
import com.willfp.eco.core.blocks.provider.BlockProvider
import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.items.EcoItems
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import java.util.EnumMap

/**
 * The runtime block registry, rebuilt on every reload from items with a
 * `block:` section. A world block's identity is derived purely from its
 * blockstate: backing material -> variation math -> this registry.
 */
object EcoBlocks {
    /** A custom block as found in the world. */
    data class Placed(val block: EcoBlock, val orientation: Int) {
        /** How many stacked items this state represents (1 for plain blocks). */
        val stackSize: Int
            get() = if (block.stackable != null) orientation + 1 else 1
    }

    private var byVariation = mapOf<BlockBacking, Map<Int, Placed>>()
    private var byId = mapOf<String, EcoBlock>()
    private var assignments = mapOf<String, List<Int>>()
    private val registeredKeys = mutableSetOf<NamespacedKey>()

    fun reload(plugin: EcoItemsPlugin) {
        val blocks = EcoItems.values().flatMap { listOfNotNull(it.block, it.crop?.block) }
        assignments = BlockVariations.assign(plugin, blocks)

        recommendPaperFlags(plugin, blocks)

        byId = blocks.filter { it.id in assignments }.associateBy { it.id }

        for (block in byId.values) {
            val target = block.stripsTo ?: continue
            if (target !in byId) {
                plugin.logger.warning("Block ${block.id} strips-to unknown block '$target'")
            }
        }

        val variationMaps = EnumMap<BlockBacking, MutableMap<Int, Placed>>(BlockBacking::class.java)
        for (block in byId.values) {
            val map = variationMaps.getOrPut(block.backing) { mutableMapOf() }
            for ((orientation, variation) in assignments.getValue(block.id).withIndex()) {
                map[variation] = Placed(block, orientation)
            }
        }
        byVariation = variationMaps

        // eco registration: ecoitems:<id> resolves in every plugin's block
        // lookups (libreforge filters, EcoSkills xp, ...).
        val stale = registeredKeys.toMutableSet()
        for (block in byId.values) {
            val key = plugin.namespacedKeyFactory.create(block.id)
            CustomBlock(
                key,
                { test -> at(test)?.block?.id == block.id },
                { location -> place(block, location) },
                block.hardness.toFloat()
            ).register()
            registeredKeys += key
            stale -= key
        }
        for (key in stale) {
            Blocks.removeCustomBlock(key)
            registeredKeys -= key
        }
    }

    fun values(): Collection<EcoBlock> = byId.values

    operator fun get(id: String): EcoBlock? = byId[id]

    /** The custom block at a world block, or null for vanilla blocks. */
    fun at(block: Block?): Placed? {
        if (block == null) return null
        val backing = BlockBacking.byMaterial(block.type) ?: return null
        val variation = backing.variationOf(block.blockData) ?: return null
        return lookup(backing, variation)
    }

    /** The custom block a backing variation is assigned to, if any. */
    fun lookup(backing: BlockBacking, variation: Int): Placed? =
        byVariation[backing]?.get(variation)

    /** The blockstate for a block's orientation, or null if unassigned. */
    fun blockData(block: EcoBlock, orientation: Int = 0): BlockData? {
        val variations = assignments[block.id] ?: return null
        val variation = variations.getOrNull(orientation) ?: variations.first()
        return block.backing.createBlockData(variation)
    }

    /** The assigned variations for a block (one per orientation). */
    fun variations(block: EcoBlock): List<Int> = assignments[block.id].orEmpty()

    fun place(block: EcoBlock, location: Location, orientation: Int = 0): Block {
        val worldBlock = location.block
        blockData(block, orientation)?.let { worldBlock.blockData = it }
        return worldBlock
    }

    private var recommendedPaperFlags = false

    /**
     * Paper can skip the block updates that would normalize our states,
     * which is faster and more reliable than the listener fallback -
     * recommend it once per backing in use.
     */
    private fun recommendPaperFlags(plugin: EcoItemsPlugin, blocks: Collection<EcoBlock>) {
        if (recommendedPaperFlags || blocks.isEmpty()) {
            return
        }
        recommendedPaperFlags = true

        val file = java.io.File("config/paper-global.yml")
        if (!file.exists()) {
            return
        }

        val text = file.readText()
        val flags = mapOf(
            BlockBacking.NOTEBLOCK to "disable-noteblock-updates",
            BlockBacking.STRINGBLOCK to "disable-tripwire-updates",
            BlockBacking.CHORUS to "disable-chorus-plant-updates"
        )

        val wanted = blocks.map { it.backing }.toSet()
            .mapNotNull { flags[it] }
            .filter { !Regex("$it:\\s*true").containsMatchIn(text) }

        if (wanted.isNotEmpty()) {
            plugin.logger.warning(
                "Custom blocks work best with the Paper block-updates flags: set " +
                    wanted.joinToString(", ") + " to true under block-updates in " +
                    "config/paper-global.yml and restart the server."
            )
        }
    }

    /**
     * Load-order insurance: lookups for ecoitems:<id> made before our
     * category loads miss the registry and land here.
     */
    object Provider : BlockProvider("ecoitems") {
        override fun provideForKey(key: String): TestableBlock? {
            val block = byId[key] ?: return null
            return CustomBlock(
                com.willfp.ecoitems.plugin.namespacedKeyFactory.create(block.id),
                { test -> at(test)?.block?.id == block.id },
                { location -> place(block, location) },
                block.hardness.toFloat()
            )
        }
    }
}
