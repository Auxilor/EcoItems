package com.willfp.ecoitems.blocks

import com.willfp.ecoitems.items.EcoItems
import com.willfp.ecoitems.plugin
import org.bukkit.GameEvent
import org.bukkit.Instrument
import org.bukkit.Material
import org.bukkit.SoundGroup
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.FallingBlock
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.NotePlayEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityDropItemEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.world.GenericGameEvent
import org.bukkit.event.world.StructureGrowEvent

/**
 * Stops vanilla from normalizing the hijacked blockstates. On Paper the
 * block-updates flags in paper-global.yml make most of this a no-op; these
 * listeners are the belt-and-braces fallback that also covers Spigot.
 */
object BlockPhysicsListener : Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPhysics(event: BlockPhysicsEvent) {
        val block = event.block

        // Note blocks recalculate instrument from the block below on any
        // neighbor update - cancel for all of them and re-push the stack
        // above (vertically stacked note blocks chain updates).
        if (block.type == Material.NOTE_BLOCK) {
            event.isCancelled = true
            reapplyAbove(block)
            return
        }
        val above = block.getRelative(BlockFace.UP)
        if (above.type == Material.NOTE_BLOCK) {
            event.isCancelled = true
            reapplyAbove(above)
            return
        }

        // Tripwire and chorus states only need protecting when custom.
        if ((block.type == Material.TRIPWIRE || block.type == Material.CHORUS_PLANT) &&
            EcoBlocks.at(block) != null
        ) {
            event.isCancelled = true
        }
    }

    private fun reapplyAbove(block: Block) {
        var current = block
        while (current.type == Material.NOTE_BLOCK) {
            current.setBlockData(current.blockData, false)
            current = current.getRelative(BlockFace.UP)
        }
    }

    /** Silence custom note blocks; fix the instrument for vanilla ones. */
    @EventHandler(ignoreCancelled = true)
    fun onNotePlay(event: NotePlayEvent) {
        if (EcoBlocks.at(event.block) != null) {
            event.isCancelled = true
            return
        }

        // With note block updates disabled the server never recalculates the
        // instrument from the block below, so vanilla note blocks would stay
        // on harp forever - recompute it for the sound.
        event.instrument = instrumentFor(event.block.getRelative(BlockFace.DOWN))
    }

    /**
     * A note play on a custom note block (punching plays a note) schedules a
     * state reset as a tune backstop. The reset must re-check the block: an
     * insta-mine removes it in the same tick, and blindly re-applying would
     * resurrect the block after its drops spawned.
     */
    @EventHandler(ignoreCancelled = true)
    fun onNoteBlockTuned(event: GenericGameEvent) {
        if (event.event != GameEvent.NOTE_BLOCK_PLAY) {
            return
        }

        val block = event.location.block
        val placed = EcoBlocks.at(block) ?: return
        val data = EcoBlocks.blockData(placed.block, placed.orientation) ?: return

        plugin.scheduler.run {
            if (block.type == placed.block.backing.material && EcoBlocks.at(block)?.block == placed.block) {
                block.setBlockData(data, false)
            }
        }
    }

    /** Moving custom states would let vanilla normalize them on re-place. */
    @EventHandler(ignoreCancelled = true)
    fun onPistonExtend(event: BlockPistonExtendEvent) {
        if (event.blocks.any { it.type == Material.NOTE_BLOCK || EcoBlocks.at(it) != null }) {
            event.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onPistonRetract(event: BlockPistonRetractEvent) {
        if (event.blocks.any { it.type == Material.NOTE_BLOCK || EcoBlocks.at(it) != null }) {
            event.isCancelled = true
        }
    }

    /** String/chorus blocks break with drops instead of washing away. */
    @EventHandler(ignoreCancelled = true)
    fun onWaterFlow(event: BlockFromToEvent) {
        val placed = EcoBlocks.at(event.toBlock) ?: return
        if (placed.block.backing.solid) {
            return
        }

        event.isCancelled = true
        event.toBlock.type = Material.AIR
        BlockListener.dropItems(placed.block, event.toBlock, null)
    }

    /**
     * Giant mushrooms grown from bonemeal are built from assorted face
     * combinations that can collide with assigned mushroom variations - bump
     * colliding pieces to the vanilla all-faces state before they're placed.
     */
    @EventHandler(ignoreCancelled = true)
    fun onStructureGrow(event: StructureGrowEvent) {
        for (state in event.blocks) {
            val backing = BlockBacking.byMaterial(state.type) ?: continue
            if (backing !in BlockBacking.mushrooms) {
                continue
            }

            val variation = backing.variationOf(state.blockData) ?: continue
            if (EcoBlocks.lookup(backing, variation) != null) {
                state.blockData = backing.material.createBlockData()
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        handleExplosion(event.blockList())
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockExplode(event: BlockExplodeEvent) {
        handleExplosion(event.blockList())
    }

    private fun handleExplosion(blocks: MutableList<Block>) {
        val iterator = blocks.iterator()
        while (iterator.hasNext()) {
            val block = iterator.next()
            val placed = EcoBlocks.at(block) ?: continue

            // Vanilla would drop nothing useful either way; handle ourselves.
            iterator.remove()
            if (!placed.block.blastResistant) {
                block.type = Material.AIR
                BlockListener.dropItems(placed.block, block, null)
            }
        }
    }

    /**
     * A vanilla note block / string / chorus item placed normally computes
     * connection state on place, which could collide with a custom
     * variation - reset it to the default state.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onVanillaPlace(event: BlockPlaceEvent) {
        if (BlockListener.placing) {
            return
        }

        val block = event.blockPlaced
        val backing = BlockBacking.byMaterial(block.type) ?: return
        if (backing.variationOf(block.blockData) != null) {
            block.setBlockData(backing.material.createBlockData(), false)
        }
    }

    /** Falling custom blocks: break of support spawns a falling entity. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onSupportBroken(event: BlockBreakEvent) {
        val above = event.block.getRelative(BlockFace.UP)
        val placed = EcoBlocks.at(above) ?: return
        if (!placed.block.falling) {
            return
        }

        plugin.scheduler.run {
            if (EcoBlocks.at(above)?.block != placed.block) {
                return@run
            }
            val data = above.blockData
            above.type = Material.AIR
            above.world.spawnFallingBlock(above.location.add(0.5, 0.0, 0.5), data)
        }
    }

    /** A falling custom block that can't land drops our item, not a vanilla one. */
    @EventHandler(ignoreCancelled = true)
    fun onFallingBlockDrop(event: EntityDropItemEvent) {
        val falling = event.entity as? FallingBlock ?: return
        val backing = BlockBacking.byMaterial(falling.blockData.material) ?: return
        val variation = backing.variationOf(falling.blockData) ?: return
        val placed = EcoBlocks.lookup(backing, variation) ?: return

        val item = EcoItems.getByID(placed.block.id)?.itemStack ?: return
        event.itemDrop.itemStack = item
    }

    /** Keep custom identity when a falling custom block lands. */
    @EventHandler(ignoreCancelled = true)
    fun onFallingBlockLand(event: EntityChangeBlockEvent) {
        val falling = event.entity as? FallingBlock ?: return
        if (BlockBacking.byMaterial(falling.blockData.material) == null) {
            return
        }

        // The landed state is the falling entity's data verbatim, which is
        // already the custom state - nothing to fix, but re-push next tick
        // in case a neighbor update normalized it.
        val block = event.block
        val data = falling.blockData
        plugin.scheduler.run {
            if (block.type == data.material) {
                block.setBlockData(data, false)
            }
        }
    }

    private fun instrumentFor(below: Block): Instrument {
        MATERIAL_INSTRUMENTS[below.type]?.let { return it }

        // SoundGroup wrappers don't guarantee equals; compare by place sound.
        return when (below.blockData.soundGroup.placeSound) {
            wood -> Instrument.BASS_GUITAR
            stone -> Instrument.BASS_DRUM
            sand -> Instrument.SNARE_DRUM
            glass -> Instrument.STICKS
            else -> Instrument.PIANO
        }
    }

    private val wood = Material.OAK_PLANKS.createBlockData().soundGroup.placeSound
    private val stone = Material.STONE.createBlockData().soundGroup.placeSound
    private val sand = Material.SAND.createBlockData().soundGroup.placeSound
    private val glass = Material.GLASS.createBlockData().soundGroup.placeSound

    private val MATERIAL_INSTRUMENTS = mapOf(
        Material.GOLD_BLOCK to Instrument.BELL,
        Material.CLAY to Instrument.FLUTE,
        Material.PACKED_ICE to Instrument.CHIME,
        Material.BONE_BLOCK to Instrument.XYLOPHONE,
        Material.IRON_BLOCK to Instrument.IRON_XYLOPHONE,
        Material.SOUL_SAND to Instrument.COW_BELL,
        Material.PUMPKIN to Instrument.DIDGERIDOO,
        Material.EMERALD_BLOCK to Instrument.BIT,
        Material.HAY_BLOCK to Instrument.BANJO,
        Material.GLOWSTONE to Instrument.PLING
    ) + Material.entries.filter { it.name.endsWith("_WOOL") }.associateWith { Instrument.GUITAR }
}
