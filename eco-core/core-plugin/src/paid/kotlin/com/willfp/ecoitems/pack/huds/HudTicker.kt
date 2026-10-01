package com.willfp.ecoitems.pack.huds

import com.willfp.eco.core.placeholder.context.placeholderContext
import com.willfp.eco.util.NumberUtils
import com.willfp.eco.util.asAudience
import com.willfp.eco.util.formatEco
import com.willfp.eco.util.toComponent
import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.huds.Hud
import com.willfp.ecoitems.huds.HudType
import com.willfp.ecoitems.huds.Huds
import com.willfp.ecoitems.nms.ActionBarDetection
import com.willfp.ecoitems.util.PlayerTickers
import com.willfp.ecoitems.util.runFor
import com.willfp.libreforge.EmptyProvidedHolder
import com.willfp.libreforge.toDispatcher
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

/**
 * Re-sends visible HUDs on a 5-tick heartbeat; each HUD sends when its own
 * update-ticks interval is due. Yields the action bar for a few seconds when
 * another plugin writes to it (detected by the packet listener).
 */
object HudTicker {
    // How long to yield after a foreign action bar message, and the window in
    // which a detected send is considered our own.
    private const val PAUSE_MS = 2700L
    private const val SELF_SEND_MS = 50L

    private const val HEARTBEAT_TICKS = 5

    private val pausedUntil = ConcurrentHashMap<UUID, Long>()
    private val selfSends = ConcurrentHashMap<UUID, Long>()

    private val shownActionBars = ConcurrentHashMap<UUID, String>()
    private val shownBossBars = ConcurrentHashMap<UUID, MutableMap<String, BossBar>>()

    // Heartbeat ticks so far, per player: on Folia each player ticks on the
    // thread that owns them, so there is no single heartbeat to count.
    private val counters = ConcurrentHashMap<UUID, Int>()

    fun start(plugin: EcoItemsPlugin) {
        ActionBarDetection.onDetect = { player -> pause(player) }

        // Tasks are cancelled by eco on reload, so this never stacks. Clear
        // anything shown for HUDs that may have changed or been removed.
        for (player in Bukkit.getOnlinePlayers()) {
            plugin.runFor(player) { hideAll(player) }
        }
        counters.clear()

        PlayerTickers.register(HEARTBEAT_TICKS.toLong()) { tick(it) }
    }

    /** Nothing can be scheduled once the plugin is shutting down, so then it all happens here. */
    fun stop(plugin: EcoItemsPlugin, shuttingDown: Boolean = false) {
        ActionBarDetection.onDetect = null

        for (player in Bukkit.getOnlinePlayers()) {
            if (shuttingDown) hideAll(player) else plugin.runFor(player) { hideAll(player) }
        }
    }

    private fun tick(player: Player) {
        val counter = counters.merge(player.uniqueId, HEARTBEAT_TICKS, Int::plus)!!

        tickActionBar(player, counter)
        tickBossBars(player, counter)
    }

    private fun tickActionBar(player: Player, counter: Int) {
        val hud = HudState.activeActionBarHud(player)?.takeIf { isVisible(player, it) }

        if (hud == null) {
            // Clear once so the old text doesn't linger for its fade time.
            if (shownActionBars.remove(player.uniqueId) != null && !isPaused(player)) {
                player.asAudience().sendActionBar(Component.empty())
            }
            return
        }

        val switched = shownActionBars[player.uniqueId] != hud.id
        if (!switched && !isDue(hud, counter)) {
            return
        }

        if (isPaused(player)) {
            return
        }

        selfSends[player.uniqueId] = System.currentTimeMillis()
        player.asAudience().sendActionBar(render(hud, player))
        shownActionBars[player.uniqueId] = hud.id
    }

    private fun tickBossBars(player: Player, counter: Int) {
        val bars = shownBossBars.getOrPut(player.uniqueId) { mutableMapOf() }

        for (hud in Huds.values()) {
            if (hud.type != HudType.BOSS_BAR) {
                continue
            }

            val visible = HudState.isBossBarVisible(player, hud) && isVisible(player, hud)
            val bar = bars[hud.id]

            when {
                visible && bar == null -> {
                    val created = BossBar.bossBar(
                        render(hud, player),
                        hud.bossBar.progress,
                        hud.bossBar.color,
                        hud.bossBar.overlay
                    )
                    player.asAudience().showBossBar(created)
                    bars[hud.id] = created
                }

                visible && bar != null && isDue(hud, counter) -> bar.name(render(hud, player))

                !visible && bar != null -> {
                    player.asAudience().hideBossBar(bar)
                    bars.remove(hud.id)
                }
            }
        }

        // Bars whose HUD no longer exists.
        val iterator = bars.iterator()
        while (iterator.hasNext()) {
            val (id, bar) = iterator.next()
            if (Huds.getByID(id) == null) {
                player.asAudience().hideBossBar(bar)
                iterator.remove()
            }
        }
    }

    private fun render(hud: Hud, player: Player): Component {
        val text = rawText(hud, player).formatEco(placeholderContext(player = player))
        var component = text.toComponent()

        if (hud.textAscent != null) {
            component = component.font(Key.key("ecoitems", "hud/${hud.id}"))
        }

        return component
    }

    private fun rawText(hud: Hud, player: Player): String {
        val frames = hud.frames?.takeIf { it.frames.isNotEmpty() } ?: return hud.text

        val value = NumberUtils.evaluateExpression(frames.value, placeholderContext(player = player))
        val span = (frames.max - frames.min).takeIf { it > 0 } ?: 1.0
        val fraction = ((value - frames.min) / span).coerceIn(0.0, 1.0)
        val frame = frames.frames[(fraction * (frames.frames.size - 1)).roundToInt()]

        return if ("%frame%" in hud.text) hud.text.replace("%frame%", frame) else frame
    }

    private fun isVisible(player: Player, hud: Hud): Boolean {
        if (hud.permission.isNotEmpty() && !player.hasPermission(hud.permission)) {
            return false
        }

        return hud.conditions.areMet(player.toDispatcher(), EmptyProvidedHolder)
    }

    private fun isDue(hud: Hud, counter: Int): Boolean =
        counter % hud.updateTicks.coerceAtLeast(HEARTBEAT_TICKS) < HEARTBEAT_TICKS

    private fun isPaused(player: Player): Boolean =
        (pausedUntil[player.uniqueId] ?: 0) > System.currentTimeMillis()

    private fun pause(player: Player) {
        val now = System.currentTimeMillis()

        // Our own sends come back through the packet listener too.
        if (now - (selfSends[player.uniqueId] ?: 0) < SELF_SEND_MS) {
            return
        }

        pausedUntil[player.uniqueId] = now + PAUSE_MS
    }

    private fun hideAll(player: Player) {
        shownActionBars.remove(player.uniqueId)
        shownBossBars.remove(player.uniqueId)?.values?.forEach {
            player.asAudience().hideBossBar(it)
        }
    }

    object QuitListener : Listener {
        @EventHandler
        fun onQuit(event: PlayerQuitEvent) {
            hideAll(event.player)
            counters.remove(event.player.uniqueId)
            pausedUntil.remove(event.player.uniqueId)
            selfSends.remove(event.player.uniqueId)
        }
    }
}
