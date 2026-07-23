package com.willfp.ecoitems.crops

import com.willfp.eco.util.namespacedKeyOf
import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.blocks.BlockListener
import com.willfp.ecoitems.blocks.EcoBlocks
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.persistence.PersistentDataType

/**
 * Planted crop positions, stored per chunk in its PDC as "x,y,z|epochSec"
 * entries (the stage itself lives in the blockstate). A timer sweeps loaded
 * chunks and advances due stages; time keeps accruing while chunks are
 * unloaded, so crops catch up when they load.
 */
object CropTracker {
    private val key = namespacedKeyOf("ecoitems", "crops")

    private const val INTERVAL_TICKS = 100L

    fun start(plugin: EcoItemsPlugin) {
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

    /** Restarts the growth timer (bonemeal advanced the stage manually). */
    fun resetTimer(block: Block) = add(block)

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

    /** The updated entry, or null when the crop is gone. */
    private fun tickEntry(chunk: Chunk, entry: String): String? {
        val (coords, timestamp) = entry.split("|", limit = 2).takeIf { it.size == 2 } ?: return null
        val (x, y, z) = coords.split(",").mapNotNull { it.toIntOrNull() }.takeIf { it.size == 3 } ?: return null
        val last = timestamp.toLongOrNull() ?: return null

        val block = chunk.world.getBlockAt(x, y, z)
        val placed = EcoBlocks.at(block)
        val crop = placed?.block?.crop ?: return null

        // Lost support (trampled farmland, broken block below) pops the crop.
        val below = block.getRelative(BlockFace.DOWN)
        val supported = if (crop.requiresFarmland) below.type == Material.FARMLAND else below.type.isSolid
        if (!supported) {
            block.type = Material.AIR
            BlockListener.dropItems(placed, block, null)
            return null
        }

        val stage = placed.orientation
        if (stage >= crop.stages.lastIndex) {
            return entry
        }

        // Too dark: pause (the timer restarts rather than accruing).
        if (block.lightLevel < crop.minLight) {
            return "${prefix(block)}${now()}"
        }

        // Weather speeds growth up (or slows it): the stage time divides by
        // the multiplier for the weather at the crop right now.
        val world = chunk.world
        val multiplier = when {
            !world.hasStorm() && !world.isThundering -> 1.0
            world.isThundering -> crop.thunderMultiplier
            block.temperature < 0.15 -> crop.snowMultiplier
            else -> crop.rainMultiplier
        }.coerceAtLeast(0.01)

        val perStage = (crop.perStageSeconds / multiplier).toLong().coerceAtLeast(1)

        val elapsed = now() - last
        if (elapsed < perStage) {
            return entry
        }

        val advance = (elapsed / perStage).toInt()
            .coerceAtMost(crop.stages.lastIndex - stage)
        EcoBlocks.blockData(placed.block, stage + advance)?.let { block.setBlockData(it, false) }

        return "${prefix(block)}${now()}"
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
