package com.willfp.ecoitems.util

import com.willfp.eco.core.Eco
import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.Prerequisite
import io.papermc.paper.entity.TeleportFlag
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerTeleportEvent
import java.util.concurrent.CompletableFuture

/*
 * Folia ticks each region on its own thread, and only the thread owning a
 * region may touch its blocks and entities. Off Folia there is one thread and
 * it owns everything, so every helper here collapses to the direct call.
 */

/** Whether the server is Folia. */
val isFolia: Boolean
    get() = Prerequisite.HAS_FOLIA.isMet

/**
 * Runs [action] now when this thread owns [entity] - always, off Folia - and
 * otherwise next tick on the thread that does.
 */
fun EcoPlugin.runFor(entity: Entity, action: () -> Unit) {
    if (Eco.get().isOwnedByCurrentRegion(entity)) {
        action()
    } else {
        scheduler.on(entity).run { action() }
    }
}

/**
 * Runs [action] now when this thread owns [location] - always, off Folia -
 * and otherwise next tick on the thread that does.
 */
fun EcoPlugin.runAt(location: Location, action: () -> Unit) {
    if (Eco.get().isOwnedByCurrentRegion(location)) {
        action()
    } else {
        scheduler.at(location).run { action() }
    }
}

/** Whether this thread may touch the block at [location]. Always true off Folia. */
fun ownsRegion(location: Location): Boolean =
    Eco.get().isOwnedByCurrentRegion(location)

/** Whether this thread may touch [entity]. Always true off Folia. */
fun ownsRegion(entity: Entity): Boolean =
    Eco.get().isOwnedByCurrentRegion(entity)

/**
 * Teleports, completing with whether it worked. Folia only allows async
 * teleports; elsewhere this is the plain synchronous teleport, already
 * complete when it returns.
 */
fun Entity.teleportSafely(location: Location, vararg flags: TeleportFlag): CompletableFuture<Boolean> {
    if (isFolia) {
        return teleportAsync(location, PlayerTeleportEvent.TeleportCause.PLUGIN, *flags)
    }

    return CompletableFuture.completedFuture(teleport(location, *flags))
}

/** Runs a command as [player], on the thread that owns them. */
fun EcoPlugin.dispatchAsPlayer(player: Player, command: String) {
    runFor(player) { Bukkit.dispatchCommand(player, command) }
}

/** Runs a command as the console, which Folia only allows on the global region. */
fun EcoPlugin.dispatchAsConsole(command: String) {
    if (isFolia && !Bukkit.isGlobalTickThread()) {
        scheduler.global().run { Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command) }
    } else {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
    }
}
