package com.willfp.ecoitems.pack.huds

import com.willfp.eco.core.actionbar.PersistentActionBar
import com.willfp.eco.core.actionbar.PersistentActionBars
import com.willfp.eco.core.placeholder.context.placeholderContext
import com.willfp.eco.util.NumberUtils
import com.willfp.eco.util.asAudience
import com.willfp.eco.util.formatEco
import com.willfp.eco.util.toComponent
import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.huds.Hud
import com.willfp.ecoitems.huds.HudType
import com.willfp.ecoitems.huds.Huds
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
 * Re-sends visible boss bar HUDs on a 5-tick heartbeat; each HUD sends when its
 * own update-ticks interval is due. Action bar HUDs are eco persistent action
 * bars, re-rendered when their update-ticks interval is due.
 */
object HudTicker {
    private const val HEARTBEAT_TICKS = 5

    private const val TICK_MILLIS = 50L

    private val actionBars = mutableListOf<PersistentActionBar>()

    private val renderedActionBars = ConcurrentHashMap<UUID, RenderedActionBar>()

    private val shownBossBars = ConcurrentHashMap<UUID, MutableMap<String, BossBar>>()

    // Heartbeat ticks so far, per player: on Folia each player ticks on the
    // thread that owns them, so there is no single heartbeat to count.
    private val counters = ConcurrentHashMap<UUID, Int>()

    fun start(plugin: EcoItemsPlugin) {
        registerActionBars(plugin)

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
        unregisterActionBars()

        for (player in Bukkit.getOnlinePlayers()) {
            if (shuttingDown) hideAll(player) else plugin.runFor(player) { hideAll(player) }
        }
    }

    private fun tick(player: Player) {
        val counter = counters.merge(player.uniqueId, HEARTBEAT_TICKS, Int::plus)!!

        tickBossBars(player, counter)
    }

    private fun registerActionBars(plugin: EcoItemsPlugin) {
        unregisterActionBars()

        for (hud in Huds.values()) {
            if (hud.type != HudType.ACTION_BAR) {
                continue
            }

            actionBars += PersistentActionBars.register(plugin, "hud_${hud.id}", hud.priority) {
                renderActionBar(it, hud)
            }
        }
    }

    private fun unregisterActionBars() {
        actionBars.forEach { it.unregister() }
        actionBars.clear()
        renderedActionBars.clear()
    }

    private fun renderActionBar(player: Player, hud: Hud): Component? {
        if (HudState.activeActionBarHud(player)?.id != hud.id || !isVisible(player, hud)) {
            return null
        }

        val now = System.currentTimeMillis()
        val rendered = renderedActionBars[player.uniqueId]

        if (rendered != null && rendered.hudId == hud.id &&
            now - rendered.renderedAt < hud.updateTicks.coerceAtLeast(HEARTBEAT_TICKS) * TICK_MILLIS
        ) {
            return rendered.component
        }

        return render(hud, player).also {
            renderedActionBars[player.uniqueId] = RenderedActionBar(hud.id, it, now)
        }
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

    private fun hideAll(player: Player) {
        renderedActionBars.remove(player.uniqueId)
        shownBossBars.remove(player.uniqueId)?.values?.forEach {
            player.asAudience().hideBossBar(it)
        }
    }

    object QuitListener : Listener {
        @EventHandler
        fun onQuit(event: PlayerQuitEvent) {
            hideAll(event.player)
            counters.remove(event.player.uniqueId)
        }
    }

    private class RenderedActionBar(
        val hudId: String,
        val component: Component,
        val renderedAt: Long
    )
}
