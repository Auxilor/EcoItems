package com.willfp.ecoitems.blocks

import org.bukkit.Instrument
import org.bukkit.Material
import org.bukkit.Note
import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.MultipleFacing
import org.bukkit.block.data.type.NoteBlock
import org.bukkit.block.data.type.Tripwire

/**
 * The vanilla blocks custom blocks hijack. Each backing maps a "variation"
 * number to one unused blockstate permutation - the state itself is the
 * block's identity, so no per-location storage is needed. The state math
 * matches Oraxen/Nexo exactly so worlds and configs can migrate.
 */
@Suppress("DEPRECATION") // Instrument type ids are the vanilla state values.
enum class BlockBacking(val material: Material, val variations: IntRange) {
    /**
     * instrument (16) x note (25) x powered (2). Variation + 26 skips the
     * states a fresh vanilla note block can occupy; the modulo wrap above
     * raw state 799 makes Oraxen's tail variations (774/775) ambiguous, so
     * we stop at 773.
     */
    NOTEBLOCK(Material.NOTE_BLOCK, 0..773) {
        override fun createBlockData(variation: Int): BlockData {
            val raw = variation + 26
            return (material.createBlockData() as NoteBlock).apply {
                instrument = Instrument.getByType(((raw % 400) / 25).toByte())!!
                note = Note(raw % 25)
                isPowered = raw >= 400
            }
        }

        override fun variationOf(data: BlockData): Int? {
            val noteBlock = data as? NoteBlock ?: return null
            val raw = noteBlock.instrument.type * 25 + noteBlock.note.id + if (noteBlock.isPowered) 400 else 0
            return (raw - 26).takeIf { it in variations }
        }
    },

    /**
     * Seven boolean properties packed into a 7-bit code. Code 0 (a plain
     * placed string) stays vanilla.
     */
    STRINGBLOCK(Material.TRIPWIRE, 1..127) {
        override fun createBlockData(variation: Int): BlockData {
            return (material.createBlockData() as Tripwire).apply {
                setFace(BlockFace.EAST, variation and 1 != 0)
                setFace(BlockFace.WEST, variation and 2 != 0)
                setFace(BlockFace.SOUTH, variation and 4 != 0)
                setFace(BlockFace.NORTH, variation and 8 != 0)
                isAttached = variation and 16 != 0
                isDisarmed = variation and 32 != 0
                isPowered = variation and 64 != 0
            }
        }

        override fun variationOf(data: BlockData): Int? {
            val tripwire = data as? Tripwire ?: return null
            var code = 0
            if (tripwire.hasFace(BlockFace.EAST)) code = code or 1
            if (tripwire.hasFace(BlockFace.WEST)) code = code or 2
            if (tripwire.hasFace(BlockFace.SOUTH)) code = code or 4
            if (tripwire.hasFace(BlockFace.NORTH)) code = code or 8
            if (tripwire.isAttached) code = code or 16
            if (tripwire.isDisarmed) code = code or 32
            if (tripwire.isPowered) code = code or 64
            return code.takeIf { it in variations }
        }
    },

    /**
     * Six connection faces packed into a 6-bit code. Code 0 (an isolated
     * plant) stays vanilla.
     */
    CHORUS(Material.CHORUS_PLANT, 1..63) {
        override fun createBlockData(variation: Int): BlockData {
            return (material.createBlockData() as MultipleFacing).apply {
                setFace(BlockFace.NORTH, variation and 1 != 0)
                setFace(BlockFace.SOUTH, variation and 2 != 0)
                setFace(BlockFace.EAST, variation and 4 != 0)
                setFace(BlockFace.WEST, variation and 8 != 0)
                setFace(BlockFace.UP, variation and 16 != 0)
                setFace(BlockFace.DOWN, variation and 32 != 0)
            }
        }

        override fun variationOf(data: BlockData): Int? {
            val facing = data as? MultipleFacing ?: return null
            var code = 0
            if (facing.hasFace(BlockFace.NORTH)) code = code or 1
            if (facing.hasFace(BlockFace.SOUTH)) code = code or 2
            if (facing.hasFace(BlockFace.EAST)) code = code or 4
            if (facing.hasFace(BlockFace.WEST)) code = code or 8
            if (facing.hasFace(BlockFace.UP)) code = code or 16
            if (facing.hasFace(BlockFace.DOWN)) code = code or 32
            return code.takeIf { it in variations }
        }
    };

    /** The blockstate for a variation. */
    abstract fun createBlockData(variation: Int): BlockData

    /** The variation a blockstate encodes, or null if it's a vanilla state. */
    abstract fun variationOf(data: BlockData): Int?

    /** The yml section name, also used in block-variations.yml. */
    val id = name.lowercase()

    companion object {
        fun byMaterial(material: Material): BlockBacking? =
            entries.firstOrNull { it.material == material }

        fun parse(value: String): BlockBacking? = when (value.lowercase()) {
            "", "noteblock", "note_block" -> NOTEBLOCK
            "stringblock", "string_block", "string", "tripwire" -> STRINGBLOCK
            "chorus", "chorusblock", "chorus_block" -> CHORUS
            else -> null
        }
    }
}
