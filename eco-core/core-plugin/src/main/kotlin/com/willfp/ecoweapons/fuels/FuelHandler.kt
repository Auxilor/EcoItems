package com.willfp.ecoweapons.fuels

import com.willfp.eco.core.EcoPlugin
import com.willfp.ecoweapons.weapons.Weapon
import com.willfp.libreforge.events.EffectActivateEvent
import com.willfp.libreforge.updateEffects
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class FuelHandler : Listener {
    @EventHandler
    fun onUseWeapon(event: EffectActivateEvent) {
        val weapon = event.holder as? Weapon ?: return
        if (weapon.fuels.isEmpty()) {
            return
        }

        queue[event.player] = weapon
    }

    companion object {
        private val queue = mutableMapOf<Player, Weapon>()

        private fun consumeFuel() {
            for ((player, weapon) in queue.toMap()) {
                fuelIter@ for (fuel in weapon.fuels) {
                    for (i in player.inventory.contents.indices) {
                        val itemStack = player.inventory.getItem(i) ?: continue

                        if (weapon.fuels.contains(FuelUtils.getFuelFromItem(itemStack))) {
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