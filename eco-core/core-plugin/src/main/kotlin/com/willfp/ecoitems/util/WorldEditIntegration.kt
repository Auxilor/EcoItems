package com.willfp.ecoitems.util

import com.willfp.ecoitems.blocks.EcoBlocks
import com.willfp.ecoitems.plugin
import org.bukkit.Bukkit
import org.bukkit.Location
import java.io.File

/**
 * WorldEdit/FAWE support: `ecoitems:<id>` resolves in block patterns
 * (`//set ecoitems:ruby_block`), and schematics can be pasted (used by
 * saplings). Only [Hook] touches WorldEdit types, so nothing classloads
 * without the plugin present.
 */
object WorldEditIntegration {
    val present: Boolean by lazy {
        Bukkit.getPluginManager().getPlugin("WorldEdit") != null ||
            Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") != null
    }

    fun register() {
        if (!present) {
            return
        }

        runCatching { Hook.register() }.onFailure {
            plugin.logger.warning("Could not hook into WorldEdit: $it")
        }
    }

    /**
     * Pastes a .schem at a location (air skipped). With [requireSpace], the
     * paste only happens when every non-air block lands on something
     * replaceable (air, plants, leaves); false when blocked or failed.
     */
    fun pasteSchematic(file: File, location: Location, requireSpace: Boolean = false): Boolean {
        if (!present) {
            return false
        }

        return runCatching { Hook.paste(file, location, requireSpace) }.getOrElse {
            plugin.logger.warning("Could not paste ${file.name}: $it")
            false
        }
    }

    /** The schematic's size as (width, height, length), or null unreadable. */
    fun schematicSize(file: File): Triple<Int, Int, Int>? {
        if (!present) {
            return null
        }

        return runCatching { Hook.size(file) }.getOrNull()
    }

    private object Hook {
        fun register() {
            com.sk89q.worldedit.WorldEdit.getInstance().blockFactory.register(Parser)
        }

        fun paste(file: File, location: Location, requireSpace: Boolean): Boolean {
            val clipboard = load(file) ?: return false

            if (requireSpace && !fits(clipboard, location)) {
                return false
            }

            com.sk89q.worldedit.WorldEdit.getInstance().newEditSessionBuilder()
                .world(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(location.world))
                .build()
                .use { session ->
                    com.sk89q.worldedit.function.operation.Operations.complete(
                        com.sk89q.worldedit.session.ClipboardHolder(clipboard)
                            .createPaste(session)
                            .to(
                                com.sk89q.worldedit.math.BlockVector3.at(
                                    location.blockX, location.blockY, location.blockZ
                                )
                            )
                            .ignoreAirBlocks(true)
                            .build()
                    )
                }
            return true
        }

        fun size(file: File): Triple<Int, Int, Int>? {
            val clipboard = load(file) ?: return null
            val dimensions = clipboard.dimensions
            return Triple(dimensions.x(), dimensions.y(), dimensions.z())
        }

        /** Every non-air clipboard block must land on something replaceable. */
        private fun fits(
            clipboard: com.sk89q.worldedit.extent.clipboard.Clipboard,
            location: Location
        ): Boolean {
            val origin = clipboard.origin
            val world = location.world ?: return false

            for (point in clipboard.region) {
                if (clipboard.getBlock(point).blockType.material.isAir) {
                    continue
                }

                val target = world.getBlockAt(
                    location.blockX + point.x() - origin.x(),
                    location.blockY + point.y() - origin.y(),
                    location.blockZ + point.z() - origin.z()
                )

                val replaceable = target.type.isAir ||
                    target.isLiquid ||
                    org.bukkit.Tag.LEAVES.isTagged(target.type) ||
                    org.bukkit.Tag.REPLACEABLE.isTagged(target.type)
                if (!replaceable) {
                    return false
                }
            }

            return true
        }

        private fun load(file: File): com.sk89q.worldedit.extent.clipboard.Clipboard? {
            val format = com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats.findByFile(file) ?: return null
            return file.inputStream().use { stream ->
                format.getReader(stream).use { it.read() }
            }
        }

        private object Parser : com.sk89q.worldedit.internal.registry.InputParser<
            com.sk89q.worldedit.world.block.BaseBlock
        >(com.sk89q.worldedit.WorldEdit.getInstance()) {
            override fun parseFromInput(
                input: String,
                context: com.sk89q.worldedit.extension.input.ParserContext
            ): com.sk89q.worldedit.world.block.BaseBlock? {
                val id = input.removePrefix("ecoitems:")
                if (id == input) {
                    return null
                }

                val block = EcoBlocks[id.lowercase()] ?: return null
                val data = EcoBlocks.blockData(block) ?: return null

                return com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(data).toBaseBlock()
            }

            override fun getSuggestions(
                input: String,
                context: com.sk89q.worldedit.extension.input.ParserContext
            ): java.util.stream.Stream<String> =
                EcoBlocks.values()
                    .map { "ecoitems:${it.id}" }
                    .filter { it.startsWith(input) }
                    .stream()
        }
    }
}
