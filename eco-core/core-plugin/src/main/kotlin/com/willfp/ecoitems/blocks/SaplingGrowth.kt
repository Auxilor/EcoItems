package com.willfp.ecoitems.blocks

import com.willfp.eco.util.namespacedKeyOf
import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.plugin
import com.willfp.ecoitems.util.WorldEditIntegration
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.SoundCategory
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.persistence.PersistentDataType

/**
 * Sapling blocks grow into pasted schematics: positions live per chunk in
 * its PDC as "x,y,z|epochSec" (like crops), a timer sweeps loaded chunks,
 * and bonemeal grows immediately. Needs WorldEdit for the paste.
 */
object SaplingGrowth : Listener {
    private val key = namespacedKeyOf("ecoitems", "saplings")

    private const val INTERVAL_TICKS = 100L

    private val warnedMissing = mutableSetOf<String>()

    fun start(plugin: EcoItemsPlugin) {
        plugin.dataFolder.resolve("schematics").mkdirs()

        if (!WorldEditIntegration.present &&
            EcoBlocks.values().any { it.sapling != null }
        ) {
            plugin.logger.warning("Sapling blocks need WorldEdit (or FAWE) installed to grow")
        }

        // eco cancels plugin tasks on reload, so this never stacks.
        plugin.scheduler.runTimer(INTERVAL_TICKS, INTERVAL_TICKS) {
            for (world in Bukkit.getWorlds()) {
                for (chunk in world.loadedChunks) {
                    tickChunk(chunk)
                }
            }
        }
    }

    fun add(block: Block) = mutate(block.chunk) { entries ->
        entries.filterNot { it.startsWith(prefix(block)) } + "${prefix(block)}${now()}"
    }

    fun remove(block: Block) = mutate(block.chunk) { entries ->
        entries.filterNot { it.startsWith(prefix(block)) }
    }

    /** Grows now if light and space allow; true when the sapling is gone. */
    fun grow(block: Block, placed: EcoBlocks.Placed): Boolean {
        val sapling = placed.block.sapling ?: return false

        if (block.lightLevel < sapling.minLight || sapling.schematics.isEmpty()) {
            return false
        }

        val name = pickWeighted(sapling.schematics)
        val file = plugin.dataFolder.resolve("schematics/$name")
        if (!file.isFile) {
            if (warnedMissing.add(name)) {
                plugin.logger.warning("Sapling ${placed.block.id}: schematics/$name does not exist")
            }
            return false
        }

        // The sapling makes way for its own trunk; restored if blocked.
        val previous = block.blockData
        block.setBlockData(Material.AIR.createBlockData(), false)

        if (!WorldEditIntegration.pasteSchematic(file, block.location, sapling.requireSpace)) {
            block.setBlockData(previous, false)
            return false
        }

        remove(block)
        return true
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onBonemeal(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK || event.hand != EquipmentSlot.HAND) {
            return
        }

        val item = event.item ?: return
        if (item.type != Material.BONE_MEAL) {
            return
        }

        val block = event.clickedBlock ?: return
        val placed = EcoBlocks.at(block) ?: return
        val sapling = placed.block.sapling ?: return

        event.isCancelled = true

        if (!sapling.bonemeal) {
            return
        }

        if (event.player.gameMode != GameMode.CREATIVE) {
            item.amount -= 1
        }
        event.player.swingMainHand()
        block.world.spawnParticle(Particle.HAPPY_VILLAGER, block.location.add(0.5, 0.5, 0.5), 12, 0.3, 0.3, 0.3)
        block.world.playSound(
            block.location.add(0.5, 0.5, 0.5),
            "minecraft:item.bone_meal.use",
            SoundCategory.BLOCKS,
            1.0f,
            1.0f
        )

        grow(block, placed)
    }

    /** Player breaks go through the block path; just forget the sapling. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        val placed = EcoBlocks.at(event.block) ?: return
        if (placed.block.sapling != null) {
            remove(event.block)
        }
    }

    private fun tickChunk(chunk: Chunk) {
        val raw = chunk.persistentDataContainer.get(key, PersistentDataType.STRING) ?: return

        var changed = false
        val updated = raw.split(";").mapNotNull { entry ->
            val result = tickEntry(chunk, entry)
            if (result != entry) {
                changed = true
            }
            result
        }

        if (changed) {
            write(chunk, updated)
        }
    }

    /** The updated entry, or null when the sapling is gone. */
    private fun tickEntry(chunk: Chunk, entry: String): String? {
        val (coords, timestamp) = entry.split("|", limit = 2).takeIf { it.size == 2 } ?: return null
        val (x, y, z) = coords.split(",").mapNotNull { it.toIntOrNull() }.takeIf { it.size == 3 } ?: return null
        val last = timestamp.toLongOrNull() ?: return null

        val block = chunk.world.getBlockAt(x, y, z)
        val placed = EcoBlocks.at(block)
        val sapling = placed?.block?.sapling ?: return null

        if (sapling.growthTime <= 0) {
            return entry
        }

        if (now() - last < sapling.growthTime) {
            return entry
        }

        // Blocked or dark: try again after another interval.
        return if (grow(block, placed)) null else "${prefix(block)}${now()}"
    }

    private fun pickWeighted(schematics: List<Pair<String, Double>>): String {
        val total = schematics.sumOf { it.second.coerceAtLeast(0.0) }
        var roll = Math.random() * total
        for ((name, weight) in schematics) {
            roll -= weight.coerceAtLeast(0.0)
            if (roll <= 0) {
                return name
            }
        }
        return schematics.last().first
    }

    private fun mutate(chunk: Chunk, transform: (List<String>) -> List<String>) {
        val raw = chunk.persistentDataContainer.get(key, PersistentDataType.STRING)
        val entries = raw?.split(";")?.filter { it.isNotBlank() } ?: emptyList()
        write(chunk, transform(entries))
    }

    private fun write(chunk: Chunk, entries: List<String>) {
        if (entries.isEmpty()) {
            chunk.persistentDataContainer.remove(key)
        } else {
            chunk.persistentDataContainer.set(key, PersistentDataType.STRING, entries.joinToString(";"))
        }
    }

    private fun prefix(block: Block) = "${block.x},${block.y},${block.z}|"

    private fun now() = System.currentTimeMillis() / 1000
}
