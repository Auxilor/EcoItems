package com.willfp.ecoitems.pack.delivery

import com.willfp.ecoitems.plugin
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerResourcePackStatusEvent

object PackListener : Listener {
    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val settings = PackDelivery.settings ?: return
        if (!settings.sendOnJoin || PackDelivery.current == null) {
            return
        }

        val player = event.player
        plugin.scheduler.runLater(maxOf(1, settings.joinDelayTicks).toLong()) {
            if (player.isOnline) {
                PackDelivery.send(player)
            }
        }
    }

    @EventHandler
    fun onStatus(event: PlayerResourcePackStatusEvent) {
        val settings = PackDelivery.settings ?: return
        if (!settings.required || !settings.kickOnDecline) {
            return
        }

        if (event.status == PlayerResourcePackStatusEvent.Status.DECLINED) {
            event.player.kickPlayer(plugin.langYml.getMessage("pack-declined"))
        }
    }
}
