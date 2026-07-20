package com.willfp.ecoitems.furniture

import com.willfp.eco.core.items.Items
import com.willfp.eco.util.asAudience
import com.willfp.eco.util.formatEco
import com.willfp.eco.util.toComponent
import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.items.EcoItems
import com.willfp.ecoitems.plugin
import com.willfp.ecoitems.util.WorldGuardFlags
import io.papermc.paper.entity.TeleportFlag
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

/**
 * Drives vehicle furniture. The player in the first seat steers with their
 * look direction and WASD (read from Paper's input API - vehicles need Paper
 * 1.21.2+ and quietly stay parked elsewhere). The whole entity stack
 * (display, hitboxes, seats) translates together each tick.
 */
object VehicleTicker {
    private const val GRAVITY = 0.4
    private const val BACKWARD_FACTOR = 0.4

    private val supported = runCatching {
        Player::class.java.getMethod("getCurrentInput")
    }.isSuccess

    private val fuelClock = mutableMapOf<UUID, Long>()
    private val lastFuelWarning = mutableMapOf<UUID, Long>()

    fun start(plugin: EcoItemsPlugin) {
        val anyVehicles = EcoItems.values().any { it.furniture?.vehicle != null }
        if (!anyVehicles) {
            return
        }

        if (!supported) {
            plugin.logger.warning("Vehicle furniture needs Paper 1.21.2+ for player input; vehicles stay parked")
            return
        }

        // eco cancels plugin tasks on reload, so this never stacks.
        plugin.scheduler.runTimer(1, 1) {
            for (player in Bukkit.getOnlinePlayers()) {
                drive(player)
            }
        }
    }

    private fun drive(player: Player) {
        val stand = player.vehicle as? ArmorStand ?: run {
            fuelClock.remove(player.uniqueId)
            return
        }
        val placed = PlacedFurniture.fromEntity(stand) ?: return
        val furniture = placed.furniture ?: return
        val vehicle = furniture.vehicle ?: return

        // Only the first seat drives.
        if (placed.seatEntities().firstOrNull()?.uniqueId != stand.uniqueId) {
            return
        }

        if (!WorldGuardFlags.test(player, placed.base.location, WorldGuardFlags.VEHICLE)) {
            return
        }

        val input = player.currentInput
        var throttle = 0.0
        if (input.isForward) throttle += 1.0
        if (input.isBackward) throttle -= BACKWARD_FACTOR

        val flying = vehicle.flySpeed > 0
        var vertical = 0.0
        if (flying && input.isJump) {
            vertical = vehicle.flySpeed
        }

        val base = placed.base
        val from = base.location

        if ((throttle != 0.0 || vertical != 0.0) && !consumeFuel(player, vehicle)) {
            return
        }

        val radians = Math.toRadians(player.location.yaw.toDouble())
        var target = from.clone().add(
            -sin(radians) * vehicle.speed * throttle,
            vertical,
            cos(radians) * vehicle.speed * throttle
        )

        // Two blocks of headroom, with a one-block step up on the ground.
        if (blocked(target)) {
            val stepped = target.clone().add(0.0, 1.0, 0.0)
            target = if (!blocked(stepped) && !passable(from.clone().add(0.0, -0.6, 0.0))) {
                stepped
            } else {
                from.clone().add(0.0, vertical, 0.0)
            }
        }

        // Ground vehicles fall; flying vehicles hover.
        if (!flying && passable(target.clone().add(0.0, -0.6, 0.0))) {
            target.add(0.0, -GRAVITY, 0.0)
        }

        val delta = target.clone().subtract(from)
        if (delta.lengthSquared() < 1.0E-6 && player.location.yaw == from.yaw - 180f) {
            return
        }

        val yaw = player.location.yaw

        val particle = vehicle.smokeParticle
        if (particle != null && (throttle != 0.0 || vertical != 0.0)) {
            val (sx, sz) = PlacedFurniture.rotate(vehicle.smokeOffset[0], vehicle.smokeOffset[2], yaw)
            target.world?.spawnParticle(
                particle,
                target.clone().add(sx, vehicle.smokeOffset[1], sz),
                vehicle.smokeAmount,
                0.05, 0.05, 0.05,
                0.01
            )
        }

        // Move every entity of the furniture by the same offset; the ridden
        // seat keeps its passenger through the teleport.
        for (entity in listOf(base) + placed.seatEntities() + placed.interactionEntities()) {
            val to = entity.location.add(delta.x, delta.y, delta.z)
            if (entity is ItemDisplay) {
                to.yaw = yaw + 180f
            }
            entity.teleport(to, TeleportFlag.EntityState.RETAIN_PASSENGERS)
        }
    }

    private fun blocked(displayLocation: Location): Boolean =
        !passable(displayLocation) || !passable(displayLocation.clone().add(0.0, 1.0, 0.0))

    private fun passable(location: Location): Boolean =
        location.block.isPassable

    /** True while there's fuel to burn (or none is needed). */
    private fun consumeFuel(player: Player, vehicle: FurnitureVehicle): Boolean {
        if (!vehicle.needsFuel) {
            return true
        }

        val now = System.currentTimeMillis()
        val burningSince = fuelClock[player.uniqueId]
        if (burningSince != null && now - burningSince < vehicle.fuelSeconds * 1000L) {
            return true
        }

        val testables = vehicle.fuelItems.map { Items.lookup(it) }
        for (stack in player.inventory.storageContents) {
            if (stack == null || testables.none { it.matches(stack) }) {
                continue
            }

            stack.amount -= 1
            fuelClock[player.uniqueId] = now
            return true
        }

        // Rate-limit the warning to once a second.
        if (now - (lastFuelWarning[player.uniqueId] ?: 0) > 1000) {
            lastFuelWarning[player.uniqueId] = now
            player.asAudience().sendActionBar(
                plugin.langYml.getString("messages.vehicle-no-fuel").formatEco().toComponent()
            )
        }
        return false
    }
}
