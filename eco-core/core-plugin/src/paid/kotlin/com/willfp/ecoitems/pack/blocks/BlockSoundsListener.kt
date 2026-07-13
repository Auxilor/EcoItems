package com.willfp.ecoitems.pack.blocks

import com.willfp.ecoitems.blocks.BlockListener
import com.willfp.ecoitems.blocks.BlockSoundState
import com.willfp.ecoitems.blocks.EcoBlocks
import com.willfp.ecoitems.plugin
import org.bukkit.GameEvent
import org.bukkit.Material
import org.bukkit.SoundCategory
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageAbortEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.GenericGameEvent
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

/**
 * The pack silences block.wood.* (the sounds the note block backing would
 * trigger client-side), so the server replays them: vanilla wood blocks get
 * the replayable copies, custom blocks get their configured sounds. Only
 * active when the pack was built with the silence entries.
 */
object BlockSoundsListener : Listener {
    private val woodPlaceSound = Material.OAK_PLANKS.createBlockData().soundGroup.placeSound
    private val hitLoops = mutableMapOf<UUID, BukkitTask>()

    @EventHandler(ignoreCancelled = true)
    fun onStepOrFall(event: GenericGameEvent) {
        if (!BlockSoundState.remapActive) return
        if (event.event != GameEvent.STEP && event.event != GameEvent.HIT_GROUND) return
        // Players only: mobs stepping everywhere would be a lot of lookups.
        if (event.entity !is Player) return

        val block = event.location.block.let {
            if (it.type.isAir) it.getRelative(BlockFace.DOWN) else it
        }
        val fall = event.event == GameEvent.HIT_GROUND

        val placed = EcoBlocks.at(block)
        val sound = when {
            placed != null -> {
                val custom = if (fall) placed.block.sounds?.fall else placed.block.sounds?.step
                custom ?: if (placed.block.backing.material == Material.NOTE_BLOCK) {
                    "minecraft:block.wood.${if (fall) "fall" else "step"}"
                } else {
                    return
                }
            }

            isWood(block) -> "minecraft:block.wood.${if (fall) "fall" else "step"}"

            else -> return
        }

        play(
            block, sound,
            volume = if (fall) 0.5 else 0.15,
            pitch = if (fall) 0.75 else 1.0
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onVanillaWoodPlace(event: BlockPlaceEvent) {
        if (!BlockSoundState.remapActive || BlockListener.placing) return
        val block = event.blockPlaced
        if (EcoBlocks.at(block) != null || !isWood(block)) return

        play(block, "minecraft:block.wood.place", 1.0, 0.8)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onVanillaWoodBreak(event: BlockBreakEvent) {
        if (!BlockSoundState.remapActive) return
        val block = event.block
        if (EcoBlocks.at(block) != null || !isWood(block)) return

        play(block, "minecraft:block.wood.break", 1.0, 0.8)
    }

    /** The client's mining hit loop is silenced too - replay it. */
    @EventHandler(ignoreCancelled = true)
    fun onStartMining(event: BlockDamageEvent) {
        if (!BlockSoundState.remapActive || event.instaBreak) return

        val block = event.block
        val placed = EcoBlocks.at(block)
        val sound = when {
            placed != null ->
                placed.block.sounds?.hit
                    ?: "minecraft:block.wood.hit".takeIf { placed.block.backing.material == Material.NOTE_BLOCK }
                    ?: return

            isWood(block) -> "minecraft:block.wood.hit"

            else -> return
        }

        stopHitLoop(event.player)
        hitLoops[event.player.uniqueId] = plugin.scheduler.runTimer(4, 4) {
            play(block, sound, 0.25, 0.5)
        }
    }

    @EventHandler
    fun onAbortMining(event: BlockDamageAbortEvent) = stopHitLoop(event.player)

    @EventHandler(priority = EventPriority.MONITOR)
    fun onBreakBlock(event: BlockBreakEvent) = stopHitLoop(event.player)

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) = stopHitLoop(event.player)

    private fun stopHitLoop(player: Player) {
        hitLoops.remove(player.uniqueId)?.cancel()
    }

    private fun isWood(block: Block): Boolean =
        block.type != Material.NOTE_BLOCK &&
            block.blockData.soundGroup.placeSound == woodPlaceSound

    private fun play(block: Block, sound: String, volume: Double, pitch: Double) {
        block.world.playSound(
            block.location.add(0.5, 0.5, 0.5),
            BlockListener.remapSilencedSound(sound),
            SoundCategory.BLOCKS,
            volume.toFloat(),
            pitch.toFloat()
        )
    }
}
