package com.willfp.ecoitems.util

import io.papermc.paper.entity.TeleportFlag
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.event.player.PlayerTeleportEvent
import java.util.concurrent.CompletableFuture

/** The only object touching Paper's teleport flags, so Spigot never loads them. */
internal object PaperTeleports {
    @Suppress("F004")
    fun teleportWithPassengers(entity: Entity, location: Location): CompletableFuture<Boolean> {
        if (isFolia) {
            return entity.teleportAsync(
                location,
                PlayerTeleportEvent.TeleportCause.PLUGIN,
                TeleportFlag.EntityState.RETAIN_PASSENGERS
            )
        }

        return CompletableFuture.completedFuture(
            entity.teleport(location, TeleportFlag.EntityState.RETAIN_PASSENGERS)
        )
    }
}
