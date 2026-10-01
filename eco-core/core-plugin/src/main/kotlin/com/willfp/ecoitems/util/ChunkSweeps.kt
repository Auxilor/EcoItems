package com.willfp.ecoitems.util

import com.willfp.ecoitems.plugin
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Periodic sweeps over the loaded chunks that carry a PDC key (crops,
 * saplings).
 *
 * Off Folia one timer walks every loaded chunk. On Folia no thread may walk
 * every world's chunks, so chunks carrying a swept key are tracked as they
 * load, and each sweep ticks them on the region that owns them.
 */
object ChunkSweeps : Listener {
    private data class ChunkRef(val world: UUID, val x: Int, val z: Int)

    private val tracked = ConcurrentHashMap<NamespacedKey, MutableSet<ChunkRef>>()

    /**
     * Ticks every loaded chunk carrying [key] each [interval] ticks. eco
     * cancels plugin tasks on reload, so this never stacks.
     */
    fun start(key: NamespacedKey, interval: Long, tick: (Chunk) -> Unit) {
        if (!isFolia) {
            plugin.scheduler.global().runTimer(interval, interval) {
                for (world in Bukkit.getWorlds()) {
                    for (chunk in world.loadedChunks) {
                        tick(chunk)
                    }
                }
            }
            return
        }

        val first = tracked.putIfAbsent(key, ConcurrentHashMap.newKeySet()) == null
        if (first) {
            seed(key)
        }

        plugin.scheduler.global().runTimer(interval, interval) {
            for (ref in setFor(key).toList()) {
                val world = Bukkit.getWorld(ref.world)
                if (world == null) {
                    setFor(key).remove(ref)
                    continue
                }

                plugin.scheduler.at(world, ref.x, ref.z).run {
                    if (world.isChunkLoaded(ref.x, ref.z)) {
                        tick(world.getChunkAt(ref.x, ref.z))
                    }
                }
            }
        }
    }

    /** Marks a chunk as carrying [key], so Folia sweeps it. */
    fun track(key: NamespacedKey, chunk: Chunk) {
        if (isFolia) {
            setFor(key) += chunk.ref()
        }
    }

    /**
     * Chunks loaded before the first sweep started never fire a load event
     * we see. Folia may refuse to list them off their region; if it does,
     * they are picked up the next time they load, and growth time keeps
     * accruing meanwhile.
     */
    private fun seed(key: NamespacedKey) {
        for (world in Bukkit.getWorlds()) {
            val chunks = runCatching { world.loadedChunks.map { it.x to it.z } }.getOrNull() ?: continue

            for ((x, z) in chunks) {
                plugin.scheduler.at(world, x, z).run {
                    if (world.isChunkLoaded(x, z)) {
                        val chunk = world.getChunkAt(x, z)
                        if (chunk.persistentDataContainer.has(key)) {
                            track(key, chunk)
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    fun onLoad(event: ChunkLoadEvent) {
        if (!isFolia) {
            return
        }

        val pdc = event.chunk.persistentDataContainer
        for (key in tracked.keys) {
            if (pdc.has(key)) {
                setFor(key) += event.chunk.ref()
            }
        }
    }

    @EventHandler
    fun onUnload(event: ChunkUnloadEvent) {
        if (!isFolia) {
            return
        }

        val ref = event.chunk.ref()
        for (set in tracked.values) {
            set -= ref
        }
    }

    private fun setFor(key: NamespacedKey): MutableSet<ChunkRef> =
        tracked.computeIfAbsent(key) { ConcurrentHashMap.newKeySet() }

    private fun Chunk.ref() = ChunkRef(world.uid, x, z)
}
