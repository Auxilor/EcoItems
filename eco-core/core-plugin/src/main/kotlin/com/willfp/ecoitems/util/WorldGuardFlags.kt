package com.willfp.ecoitems.util

import com.willfp.ecoitems.plugin
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player

/**
 * Optional WorldGuard region flags, all state flags defaulting to allow:
 *
 *   ecoitems-furniture-interact  - right-clicking furniture at all
 *   ecoitems-furniture-sit       - taking a seat
 *   ecoitems-furniture-storage   - opening furniture storage
 *   ecoitems-vehicle             - driving vehicles
 *   ecoitems-block-interact      - custom block click effects
 *
 * Everything routes through reflection-free accessors in [Hook], which only
 * classloads when WorldGuard is actually present.
 */
object WorldGuardFlags {
    const val FURNITURE_INTERACT = "ecoitems-furniture-interact"
    const val FURNITURE_SIT = "ecoitems-furniture-sit"
    const val FURNITURE_STORAGE = "ecoitems-furniture-storage"
    const val VEHICLE = "ecoitems-vehicle"
    const val BLOCK_INTERACT = "ecoitems-block-interact"

    private val present: Boolean by lazy {
        Bukkit.getPluginManager().getPlugin("WorldGuard") != null
    }

    private var registered = false

    /** Must run during onLoad - WorldGuard locks its flag registry at enable. */
    fun register() {
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {
            return
        }

        runCatching {
            Hook.register()
            registered = true
        }.onFailure {
            plugin.logger.warning("Could not register WorldGuard flags: $it")
        }
    }

    /** True when the player may do this here (or WorldGuard isn't involved). */
    fun test(player: Player, location: Location, flag: String): Boolean {
        if (!present || !registered) {
            return true
        }

        return runCatching { Hook.test(player, location, flag) }.getOrDefault(true)
    }

    /** The only class that touches WorldGuard types. */
    private object Hook {
        private val flags = listOf(
            FURNITURE_INTERACT, FURNITURE_SIT, FURNITURE_STORAGE, VEHICLE, BLOCK_INTERACT
        ).associateWith { com.sk89q.worldguard.protection.flags.StateFlag(it, true) }

        fun register() {
            val registry = com.sk89q.worldguard.WorldGuard.getInstance().flagRegistry
            for (flag in flags.values) {
                runCatching { registry.register(flag) }
            }
        }

        fun test(player: Player, location: Location, flag: String): Boolean {
            val stateFlag = flags[flag] ?: return true
            val localPlayer = com.sk89q.worldguard.bukkit.WorldGuardPlugin.inst().wrapPlayer(player)

            if (com.sk89q.worldguard.WorldGuard.getInstance().platform.sessionManager
                    .hasBypass(localPlayer, localPlayer.world)
            ) {
                return true
            }

            return com.sk89q.worldguard.WorldGuard.getInstance().platform.regionContainer
                .createQuery()
                .testState(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(location), localPlayer, stateFlag)
        }
    }
}
