package com.willfp.ecoitems.blocks

import com.willfp.eco.core.Prerequisite
import io.papermc.paper.event.entity.EntityInsideBlockEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

/**
 * Paper-only guards. Only instantiate on Paper - registering this on Spigot
 * would fail reflection.
 */
class PaperBlockListener : Listener {
    /** Walking through a custom string block shouldn't trip the wire. */
    @EventHandler(ignoreCancelled = true)
    fun onEntityInside(event: EntityInsideBlockEvent) {
        if (EcoBlocks.at(event.block) != null) {
            event.isCancelled = true
        }
    }

    companion object {
        fun createIfSupported(): Listener? =
            if (Prerequisite.HAS_PAPER.isMet) PaperBlockListener() else null
    }
}
