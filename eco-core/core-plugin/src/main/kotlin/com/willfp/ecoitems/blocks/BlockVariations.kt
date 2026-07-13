package com.willfp.ecoitems.blocks

import com.willfp.eco.core.config.ConfigType
import com.willfp.eco.core.config.TransientConfig
import com.willfp.ecoitems.EcoItemsPlugin

/**
 * Assigns each block a stable variation (or one per orientation for
 * directional blocks), persisted to block-variations.yml. World blocks store
 * their identity as the blockstate itself, so an assignment must never change
 * once a block has been placed with it - assignments for deleted blocks are
 * kept so re-adding a block restores its old states.
 */
object BlockVariations {
    fun assign(plugin: EcoItemsPlugin, blocks: Collection<EcoBlock>): Map<String, List<Int>> {
        val file = plugin.dataFolder.resolve("block-variations.yml")

        val persisted = mutableMapOf<BlockBacking, MutableMap<String, List<Int>>>()
        for (backing in BlockBacking.entries) {
            persisted[backing] = sortedMapOf()
        }

        if (file.exists()) {
            val config = TransientConfig(file.readText(), ConfigType.YAML)
            for (backing in BlockBacking.entries) {
                for (id in config.getSubsection(backing.id).getKeys(false)) {
                    val variations = config.getString("${backing.id}.$id")
                        .split(",")
                        .mapNotNull { it.trim().toIntOrNull() }
                    if (variations.isNotEmpty()) {
                        persisted.getValue(backing)[id] = variations
                    }
                }
            }
        }

        var dirty = false
        val assignments = mutableMapOf<String, List<Int>>()

        for (backing in BlockBacking.entries) {
            val forBacking = blocks.filter { it.backing == backing }
            if (forBacking.isEmpty() && persisted.getValue(backing).isEmpty()) {
                continue
            }

            val stored = persisted.getValue(backing)
            val used = stored.values.flatten().toMutableSet()

            // Explicit variations win over everything and are never persisted;
            // a persisted entry under the same id is kept for if the explicit
            // key is later removed.
            val remaining = forBacking.filter { block ->
                val explicit = block.configuredVariation ?: return@filter true

                val span = explicit until explicit + block.orientations.size
                val ownStored = stored[block.id].orEmpty().toSet()
                val outOfRange = span.any { it !in backing.variations }
                val collision = span.any { it in used && it !in ownStored }

                if (outOfRange || collision) {
                    plugin.logger.warning(
                        "Block ${block.id} has variation $explicit, which is " +
                            (if (outOfRange) "outside ${backing.variations} for ${backing.id}" else "already in use") +
                            "; assigning automatically instead"
                    )
                    true
                } else {
                    assignments[block.id] = span.toList()
                    used.addAll(span)
                    false
                }
            }

            // Persisted assignments are reused when they still fit the block's
            // orientation count; otherwise (e.g. directional type changed) the
            // block is reassigned.
            var next = backing.variations.first
            fun nextFree(count: Int): List<Int>? {
                val result = mutableListOf<Int>()
                while (result.size < count) {
                    while (next in used) next++
                    if (next > backing.variations.last) {
                        return null
                    }
                    result += next
                    used += next
                    next++
                }
                return result
            }

            for (block in remaining.sortedBy { it.id }) {
                val existing = stored[block.id]
                if (existing != null && existing.size == block.orientations.size &&
                    existing.all { it in backing.variations }
                ) {
                    assignments[block.id] = existing
                    continue
                }

                val assigned = nextFree(block.orientations.size)
                if (assigned == null) {
                    plugin.logger.warning(
                        "Out of ${backing.id} states! Block ${block.id} cannot be assigned " +
                            "(${backing.variations.last - backing.variations.first + 1} available per backing)."
                    )
                    continue
                }

                if (existing != null) {
                    plugin.logger.warning(
                        "Block ${block.id} changed shape (was ${existing.size} states, now ${block.orientations.size}); " +
                            "reassigned - already-placed blocks of it will break"
                    )
                }

                assignments[block.id] = assigned
                stored[block.id] = assigned
                dirty = true
            }
        }

        if (dirty) {
            val lines = mutableListOf(
                "# Blockstate assignments. The state IS the block's identity in the world,",
                "# so entries must not be edited or removed once blocks have been placed.",
                ""
            )
            for (backing in BlockBacking.entries) {
                lines += "${backing.id}:"
                for ((id, variations) in persisted.getValue(backing)) {
                    lines += "  $id: \"${variations.joinToString(",")}\""
                }
            }
            file.writeText(lines.joinToString("\n") + "\n")
        }

        return assignments
    }
}
