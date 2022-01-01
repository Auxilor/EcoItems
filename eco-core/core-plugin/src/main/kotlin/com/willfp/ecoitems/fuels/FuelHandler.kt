package com.willfp.ecoitems.fuels

import com.willfp.eco.core.EcoPlugin
import com.willfp.ecoitems.items.EcoItem
import com.willfp.libreforge.events.EffectActivateEvent
import com.willfp.libreforge.updateEffects
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class FuelHandler : Listener {
    @EventHandler
    fun onUseItem(event: EffectActivateEvent) {
        val item = event.holder as? EcoItem ?: return
        if (item.fuels.isEmpty()) {
            return
        }

        queue[event.player] = item
    }

    companion object {
        private val queue = mutableMapOf<Player, EcoItem>()

        private fun consumeFuel() {
            for ((player, item) in queue.toMap()) {
                fuelIter@ for (fuel in item.fuels) {
                    for (i in player.inventory.contents.indices) {
                        val itemStack = player.inventory.getItem(i) ?: continue

                        if (item.fuels.contains(FuelUtils.getFuelFromItem(itemStack))) {
                            if (itemStack.amount == 1) {
                                itemStack.type = Material.AIR
                            } else {
                                itemStack.apply { amount-- }
                            }
                            player.inventory.setItem(i, itemStack)
                            player.updateEffects()
                            break@fuelIter
                        }
                    }
                }

                queue.remove(player)
            }
        }

        fun createRunnable(plugin: EcoPlugin) {
            plugin.scheduler.runTimer({ consumeFuel() }, 1, 1)
        }
    }
}