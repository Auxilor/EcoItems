package com.willfp.ecoitems.furniture

import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.nms.SleepProxy
import com.willfp.ecoitems.plugin
import com.willfp.ecoitems.util.runFor
import com.willfp.ecoitems.util.teleportSafely
import org.bukkit.Bukkit
import org.bukkit.GameRule
import org.bukkit.Location
import org.bukkit.Statistic
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Furniture beds: right-clicking at night lays the player down (real
 * sleeping pose via [SleepProxy] - no bed block involved) and resets their
 * phantom timer. A timer skips the night vanilla-style once enough of the
 * world is asleep, honoring playersSleepingPercentage.
 */
object FurnitureBeds : Listener {
    private class Session(val baseId: UUID, val cell: Location)

    private val sleepers: MutableMap<UUID, Session> = ConcurrentHashMap()

    private const val SKIP_CHECK_TICKS = 100L
    private const val MORNING = 23460L

    fun start(plugin: EcoItemsPlugin) {
        // eco cancels plugin tasks on reload, so this never stacks. World
        // time and weather belong to the global region on Folia.
        plugin.scheduler.global().runTimer(SKIP_CHECK_TICKS, SKIP_CHECK_TICKS) {
            for (world in sleepers.values.mapNotNull { it.cell.world }.distinct()) {
                trySkipNight(world)
            }
        }
    }

    /** Handles a right-click on bed furniture; true = the click is consumed. */
    fun tryLie(placed: PlacedFurniture, furniture: Furniture, player: Player): Boolean {
        if (furniture.beds.isEmpty()) {
            return false
        }

        val world = player.world
        val canSleep = world.environment != World.Environment.NORMAL ||
            world.time in 12542..23459 || world.isThundering
        if (!canSleep) {
            player.sendMessage(plugin.langYml.getMessage("bed-only-at-night"))
            return true
        }

        val cell = furniture.beds.asSequence()
            .map { bed -> bedLocation(placed, bed) }
            .firstOrNull { candidate ->
                sleepers.values.none {
                    it.cell.world == candidate.world && it.cell.distanceSquared(candidate) < 0.1
                }
            } ?: run {
            player.sendMessage(plugin.langYml.getMessage("bed-occupied"))
            return true
        }

        sleepers[player.uniqueId] = Session(placed.base.uniqueId, cell)

        player.teleportSafely(cell).thenAccept {
            // Mid-interact sleep desyncs the client, like seat mounting.
            plugin.scheduler.on(player).run {
                if (player.isOnline && player.uniqueId in sleepers) {
                    plugin.getProxy(SleepProxy::class.java).sleep(player, cell)
                    player.setStatistic(Statistic.TIME_SINCE_REST, 0)
                }
            }
        }
        return true
    }

    fun wake(player: Player) {
        if (sleepers.remove(player.uniqueId) != null) {
            plugin.getProxy(SleepProxy::class.java).wake(player)
        }
    }

    /** Wakes everyone lying on a piece that is being removed. */
    fun wakeAllOn(baseId: UUID) {
        for ((uuid, session) in sleepers.filterValues { it.baseId == baseId }) {
            sleepers.remove(uuid)
            Bukkit.getPlayer(uuid)?.let { player ->
                plugin.runFor(player) { plugin.getProxy(SleepProxy::class.java).wake(player) }
            }
        }
    }

    private fun bedLocation(placed: PlacedFurniture, bed: Furniture.Seat): Location {
        val yaw = placed.base.location.yaw - 180f
        val (x, z) = PlacedFurniture.rotate(bed.x, bed.z, yaw)
        val origin = placed.base.location.block.location

        return origin.add(0.5 + x, bed.y + 0.5625, 0.5 + z).apply {
            this.yaw = bed.yaw ?: yaw
        }
    }

    private fun trySkipNight(world: World) {
        val night = world.time !in 0..12541 || world.isThundering
        if (!night) {
            return
        }

        // Not world.players: on Folia that list belongs to the regions.
        val players = Bukkit.getOnlinePlayers().filter { it.world == world && !it.isSleepingIgnored }
        if (players.isEmpty()) {
            return
        }

        val percentage = world.getGameRuleValue(GameRule.PLAYERS_SLEEPING_PERCENTAGE) ?: 100
        val needed = ceil(players.size * percentage / 100.0).roundToInt().coerceAtLeast(1)
        if (players.count { it.isSleeping } < needed) {
            return
        }

        world.time = MORNING
        if (world.isThundering || world.hasStorm()) {
            world.setStorm(false)
            world.isThundering = false
        }

        for (player in players.filter { it.uniqueId in sleepers }) {
            plugin.runFor(player) { wake(player) }
        }
    }

    @EventHandler
    fun onSneak(event: PlayerToggleSneakEvent) {
        if (event.isSneaking) {
            wake(event.player)
        }
    }

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        if (sleepers.isEmpty() || event.player.uniqueId !in sleepers) {
            return
        }

        if (event.from.blockX != event.to.blockX ||
            event.from.blockY != event.to.blockY ||
            event.from.blockZ != event.to.blockZ
        ) {
            wake(event.player)
        }
    }

    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        (event.entity as? Player)?.let { wake(it) }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        sleepers.remove(event.player.uniqueId)
    }
}
