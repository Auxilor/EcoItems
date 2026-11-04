package com.willfp.ecoitems.items

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent
import com.willfp.eco.core.Prerequisite
import com.willfp.ecoitems.items.KeepWhenBrokenListener.isBroken
import org.bukkit.entity.Trident
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

/** Records a thrown trident's broken state before its launch damage. Paper only. */
class BrokenTridentListener : Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onLaunch(event: PlayerLaunchProjectileEvent) {
        if (event.projectile is Trident && event.itemStack.isBroken()) {
            KeepWhenBrokenListener.markFiredFromBroken(event.projectile)
        }
    }

    companion object {
        fun createIfSupported(): Listener? =
            if (Prerequisite.HAS_PAPER.isMet) BrokenTridentListener() else null
    }
}
