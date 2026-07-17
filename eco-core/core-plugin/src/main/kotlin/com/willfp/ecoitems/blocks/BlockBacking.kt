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
enum class BlockBacking(
    val material: Material,
    val variations: IntRange,
    /** Solid backings survive water flow; the rest wash away with drops. */
    val solid: Boolean,
    /** The backing's vanilla hardness, for rescaling break speed. */
    val vanillaHardness: Double
) {
    /**
     * instrument (16) x note (25) x powered (2). Variation + 26 skips the
     * states a fresh vanilla note block can occupy; the modulo wrap above
     * raw state 799 makes Oraxen's tail variations (774/775) ambiguous, so
     * we stop at 773.
     */
    NOTEBLOCK(Material.NOTE_BLOCK, 0..773, solid = true, vanillaHardness = 0.8) {
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
    STRINGBLOCK(Material.TRIPWIRE, 1..127, solid = false, vanillaHardness = 0.0) {
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
    CHORUS(Material.CHORUS_PLANT, 1..63, solid = false, vanillaHardness = 0.4) {
        override fun createBlockData(variation: Int): BlockData = sixFaceData(material, variation)

        override fun variationOf(data: BlockData): Int? =
            sixFaceCode(data)?.takeIf { it in variations }
    },

    /**
     * Six face booleans packed like chorus. Code 63 (all faces on - the state
     * a player-placed mushroom block gets) stays vanilla. Mushroom states
     * never recalculate from neighbors, so no physics suppression is needed;
     * the catch is worldgen, which builds giant mushrooms from assorted face
     * combinations. Grown mushrooms are normalized away from assigned states,
     * but generation in mushroom-heavy biomes can still collide.
     */
    MUSHROOM(Material.BROWN_MUSHROOM_BLOCK, 0..62, solid = true, vanillaHardness = 0.2) {
        override fun createBlockData(variation: Int): BlockData = sixFaceData(material, variation)

        override fun variationOf(data: BlockData): Int? =
            sixFaceCode(data)?.takeIf { it in variations }
    },

    MUSHROOM_RED(Material.RED_MUSHROOM_BLOCK, 0..62, solid = true, vanillaHardness = 0.2) {
        override fun createBlockData(variation: Int): BlockData = sixFaceData(material, variation)

        override fun variationOf(data: BlockData): Int? =
            sixFaceCode(data)?.takeIf { it in variations }
    },

    MUSHROOM_STEM(Material.MUSHROOM_STEM, 0..62, solid = true, vanillaHardness = 0.2) {
        override fun createBlockData(variation: Int): BlockData = sixFaceData(material, variation)

        override fun variationOf(data: BlockData): Int? =
            sixFaceCode(data)?.takeIf { it in variations }
    };

    /** The blockstate for a variation. */
    abstract fun createBlockData(variation: Int): BlockData

    /** The variation a blockstate encodes, or null if it's a vanilla state. */
    abstract fun variationOf(data: BlockData): Int?

    /** The yml section name, also used in block-variations.yml. */
    val id = name.lowercase()

    companion object {
        /** The backings that hijack mushroom blocks (worldgen collides, see [MUSHROOM]). */
        val mushrooms = setOf(MUSHROOM, MUSHROOM_RED, MUSHROOM_STEM)

        fun byMaterial(material: Material): BlockBacking? =
            entries.firstOrNull { it.material == material }

        fun parse(value: String): BlockBacking? = when (value.lowercase()) {
            "", "noteblock", "note_block" -> NOTEBLOCK
            "stringblock", "string_block", "string", "tripwire" -> STRINGBLOCK
            "chorus", "chorusblock", "chorus_block" -> CHORUS
            "mushroom", "brown_mushroom", "mushroom_brown" -> MUSHROOM
            "red_mushroom", "mushroom_red" -> MUSHROOM_RED
            "mushroom_stem", "stem" -> MUSHROOM_STEM
            else -> null
        }
    }
}

/** N/S/E/W/U/D packed into bits 1/2/4/8/16/32 (chorus and mushroom blocks). */
private fun sixFaceData(material: Material, code: Int): BlockData =
    (material.createBlockData() as MultipleFacing).apply {
        setFace(BlockFace.NORTH, code and 1 != 0)
        setFace(BlockFace.SOUTH, code and 2 != 0)
        setFace(BlockFace.EAST, code and 4 != 0)
        setFace(BlockFace.WEST, code and 8 != 0)
        setFace(BlockFace.UP, code and 16 != 0)
        setFace(BlockFace.DOWN, code and 32 != 0)
    }

private fun sixFaceCode(data: BlockData): Int? {
    val facing = data as? MultipleFacing ?: return null
    var code = 0
    if (facing.hasFace(BlockFace.NORTH)) code = code or 1
    if (facing.hasFace(BlockFace.SOUTH)) code = code or 2
    if (facing.hasFace(BlockFace.EAST)) code = code or 4
    if (facing.hasFace(BlockFace.WEST)) code = code or 8
    if (facing.hasFace(BlockFace.UP)) code = code or 16
    if (facing.hasFace(BlockFace.DOWN)) code = code or 32
    return code
}
