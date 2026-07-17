package com.willfp.ecoitems.nms

import org.bukkit.Location
import org.bukkit.entity.Player

/**
 * Bed-free sleeping for furniture beds: lays a player down (sleeping pose,
 * sleep overlay) at any location - the Bukkit sleep API refuses anything
 * that isn't a real bed block.
 */
interface SleepProxy {
    fun sleep(player: Player, location: Location)

    fun wake(player: Player)
}
