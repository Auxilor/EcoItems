package com.willfp.ecoitems.pack.glyphs

import com.willfp.eco.core.config.readConfig
import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.glyphs.Glyph

/**
 * A glyph with its assigned codepoints: one frame for static glyphs, several
 * (plus a reset and offset codepoint) for animated ones.
 */
class AssignedGlyph(
    val glyph: Glyph,
    val frames: List<Int>,
    val reset: Int?,
    val offsetChar: Int?
)

/**
 * Assigns stable codepoints to glyphs, persisted to glyph-codepoints.yml so
 * glyphs keep rendering in old chat/signs/lore across reloads and config
 * reordering. Assignments for deleted glyphs are kept, so re-adding a glyph
 * restores its old codepoint.
 */
object GlyphCodepoints {
    // Off the 0xA410+ region Oraxen/Nexo auto-assign from, so imported
    // explicit chars don't fight our auto assignments. Existing persisted
    // assignments are unaffected (persisted always wins).
    private const val STATIC_BASE = 0xA800
    private const val ANIMATED_BASE = 0xE800
    private const val ANIMATED_CAP = 0xF850 // exclusive; shift chars live here

    fun assign(
        plugin: EcoItemsPlugin,
        glyphs: Collection<Glyph>,
        reserved: Set<Int> = emptySet()
    ): Map<String, AssignedGlyph> {
        val file = plugin.dataFolder.resolve("glyph-codepoints.yml")

        val staticAssignments = sortedMapOf<String, Int>()
        val animatedAssignments = sortedMapOf<String, IntRange>()

        if (file.exists()) {
            val config = file.readConfig()
            for (id in config.getSubsection("glyphs").getKeys(false)) {
                staticAssignments[id] = config.getString("glyphs.$id").toInt(16)
            }
            for (id in config.getSubsection("animations").getKeys(false)) {
                val (start, end) = config.getString("animations.$id").split("-")
                animatedAssignments[id] = start.toInt(16)..end.toInt(16)
            }
        }

        var dirty = false
        val used = mutableSetOf<Int>()
        used.addAll(staticAssignments.values)
        animatedAssignments.values.forEach { used.addAll(it) }

        // Explicit chars always win and are never persisted. Auto-assigned
        // glyphs don't care which char they hold, so an explicit char evicts
        // any auto assignment sitting on it (common after migrations - other
        // plugins assign from the same private-use region).
        val explicit = mutableMapOf<String, Int>()
        for (glyph in glyphs) {
            val configured = glyph.configuredChar ?: continue
            val codepoint = configured.codePointAt(0)

            staticAssignments.remove(glyph.id)?.let { stale ->
                used.remove(stale)
                dirty = true
            }

            for (holder in staticAssignments.filterValues { it == codepoint }.keys) {
                plugin.logger.info(
                    "Glyph ${glyph.id} has explicit char ${"%04X".format(codepoint)}; reassigning auto glyph $holder"
                )
                staticAssignments.remove(holder)
                used.remove(codepoint)
                dirty = true
            }
            for ((holder, range) in animatedAssignments.filterValues { codepoint in it }) {
                plugin.logger.info(
                    "Glyph ${glyph.id} has explicit char ${"%04X".format(codepoint)}; reassigning animated glyph $holder"
                )
                animatedAssignments.remove(holder)
                used.removeAll(range.toSet())
                dirty = true
            }

            if (!used.add(codepoint)) {
                plugin.logger.warning(
                    "Glyph ${glyph.id} has char ${"%04X".format(codepoint)} which another glyph's explicit char already uses; skipping it"
                )
                continue
            }
            explicit[glyph.id] = codepoint
        }

        // Codepoints used by imported packs' fonts: new assignments route
        // around them. Existing assignments are kept for stability, but a
        // collision means the imported pack's character wins in the merged
        // font, so warn about it.
        used.addAll(reserved)

        fun warnIfReserved(glyph: Glyph, codepoints: Iterable<Int>) {
            val collision = codepoints.firstOrNull { it in reserved } ?: return
            plugin.logger.warning(
                "Glyph ${glyph.id} uses char ${"%04X".format(collision)}, which an imported pack's font also defines; " +
                    "the imported pack wins. Delete the glyph's entry in glyph-codepoints.yml (or change its char) to re-assign it."
            )
        }

        val assigned = mutableMapOf<String, AssignedGlyph>()

        for (glyph in glyphs.sortedBy { it.id }) {
            val animation = glyph.animation

            if (animation == null) {
                val codepoint = explicit[glyph.id]
                    ?: staticAssignments[glyph.id]?.also { warnIfReserved(glyph, listOf(it)) }
                    ?: nextFree(STATIC_BASE, used).also {
                        staticAssignments[glyph.id] = it
                        used.add(it)
                        dirty = true
                    }
                if (explicit[glyph.id] != null) {
                    warnIfReserved(glyph, listOf(codepoint))
                }
                assigned[glyph.id] = AssignedGlyph(glyph, listOf(codepoint), null, null)
                continue
            }

            // Animated: a contiguous block of frames + reset + offset.
            val needed = animation.frames + 2
            var block = animatedAssignments[glyph.id]?.takeIf { it.count() >= needed }
                ?.also { warnIfReserved(glyph, it) }

            if (block == null) {
                val start = nextFreeBlock(needed, used)
                if (start == null) {
                    plugin.logger.warning("Ran out of codepoints for animated glyph ${glyph.id}; skipping it")
                    continue
                }
                block = start until start + needed
                animatedAssignments[glyph.id] = block
                used.addAll(block)
                dirty = true
            }

            val frames = (block.first until block.first + animation.frames).toList()
            assigned[glyph.id] = AssignedGlyph(
                glyph,
                frames,
                reset = block.first + animation.frames,
                offsetChar = block.first + animation.frames + 1
            )
        }

        if (dirty) {
            save(file, staticAssignments, animatedAssignments)
        }

        return assigned
    }

    private fun nextFree(from: Int, used: Set<Int>): Int {
        var codepoint = from
        while (codepoint in used) {
            codepoint++
        }
        return codepoint
    }

    private fun nextFreeBlock(size: Int, used: Set<Int>): Int? {
        var start = ANIMATED_BASE
        while (start + size <= ANIMATED_CAP) {
            val conflict = (start until start + size).firstOrNull { it in used }
            if (conflict == null) {
                return start
            }
            start = conflict + 1
        }
        return null
    }

    private fun save(
        file: java.io.File,
        static: Map<String, Int>,
        animated: Map<String, IntRange>
    ) {
        val builder = StringBuilder()
        builder.appendLine("# Managed by EcoItems. Stable codepoint assignments so glyphs keep")
        builder.appendLine("# rendering in old chat, signs, and lore across reloads.")
        builder.appendLine("# Deleting this file re-flows codepoints for auto-assigned glyphs.")
        builder.appendLine("glyphs:")
        for ((id, codepoint) in static) {
            builder.appendLine("  $id: \"${"%04X".format(codepoint)}\"")
        }
        builder.appendLine("animations:")
        for ((id, block) in animated) {
            builder.appendLine("  $id: \"${"%04X".format(block.first)}-${"%04X".format(block.last)}\"")
        }
        file.writeText(builder.toString())
    }
}
