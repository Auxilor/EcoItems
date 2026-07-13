package com.willfp.ecoitems.blocks

import io.papermc.paper.event.entity.EntityInsideBlockEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

/**
 * Paper-only guards. Only instantiate after checking the event class exists -
 * registering this on Spigot would fail reflection.
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
        fun createIfSupported(): Listener? = runCatching {
            Class.forName("io.papermc.paper.event.entity.EntityInsideBlockEvent")
            PaperBlockListener()
        }.getOrNull()
    }
}
