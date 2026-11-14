package com.willfp.ecoitems.util

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import wiki.ecohub.core.Eco
import wiki.ecohub.core.platform.ownsGlobal
import wiki.ecohub.core.plugin.EcoPlugin
import wiki.ecohub.core.plugin.startup.Prerequisite
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
 * Teleports with any passengers still riding, completing with whether it
 * worked. Folia only allows async teleports; elsewhere this is a synchronous
 * teleport, already complete when it returns. Spigot refuses to teleport an
 * entity that has passengers, so they get off and back on around it there.
 */
@Suppress("F004")
fun Entity.teleportWithPassengers(location: Location): CompletableFuture<Boolean> {
    if (Prerequisite.HAS_PAPER.isMet) {
        return PaperTeleports.teleportWithPassengers(this, location)
    }

    val riders = passengers
    riders.forEach { removePassenger(it) }
    val teleported = teleport(location)
    riders.forEach { addPassenger(it) }
    return CompletableFuture.completedFuture(teleported)
}

/** Runs a command as [player], on the thread that owns them. */
fun EcoPlugin.dispatchAsPlayer(player: Player, command: String) {
    runFor(player) { Bukkit.dispatchCommand(player, command) }
}

/** Runs a command as the console, which Folia only allows on the global region. */
fun EcoPlugin.dispatchAsConsole(command: String) {
    if (isFolia && !Bukkit.getServer().ownsGlobal()) {
        scheduler.global().run { Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command) }
    } else {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
    }
}
