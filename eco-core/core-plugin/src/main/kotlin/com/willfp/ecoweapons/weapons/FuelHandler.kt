package com.willfp.ecoweapons.weapons

import com.willfp.eco.core.EcoPlugin
import com.willfp.libreforge.events.EffectActivateEvent
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class FuelHandler : Listener {
    @EventHandler
    fun onUseWeapon(event: EffectActivateEvent) {
        val weapon = event.holder as? Weapon ?: return
        if (!weapon.fuelEnabled) {
            return
        }

        queue[event.player] = weapon
    }

    companion object {
        private val queue = mutableMapOf<Player, Weapon>()

        private fun consumeFuel() {
            for ((player, weapon) in queue.toMap()) {
                for (i in player.inventory.contents.indices) {
                    val itemStack = player.inventory.getItem(i) ?: continue
                    if (WeaponUtils.getFuelFromItem(itemStack) == weapon) {
                        if (itemStack.amount == 1) {
                            itemStack.type = Material.AIR
                        } else {
                            itemStack.apply { amount-- }
                        }
                        player.inventory.setItem(i, itemStack)
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