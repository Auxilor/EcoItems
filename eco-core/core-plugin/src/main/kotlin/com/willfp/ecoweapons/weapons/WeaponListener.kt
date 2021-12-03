package com.willfp.ecoweapons.weapons

import com.willfp.eco.core.EcoPlugin
import com.willfp.libreforge.updateEffects
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemHeldEvent

class WeaponListener(
    private val plugin: EcoPlugin
): Listener {
    @EventHandler
    fun onHoldItem(event: PlayerItemHeldEvent) {
        event.player.updateEffects()
        plugin.scheduler.run { event.player.updateEffects() }
    }
}