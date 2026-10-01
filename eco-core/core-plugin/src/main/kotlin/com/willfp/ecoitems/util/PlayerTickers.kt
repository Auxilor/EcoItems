package com.willfp.ecoitems.util

import com.willfp.ecoitems.plugin
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Repeating work done for every online player.
 *
 * Off Folia this is one timer looping the online players. On Folia no single
 * thread may touch every player, so each player gets their own timer on the
 * region that owns them - started for whoever is online now and for everyone
 * who joins later, and retired by Folia when they leave.
 *
 * eco cancels plugin tasks on reload, so tickers are dropped with [clear] at
 * the start of every reload and registered again by whatever still wants them.
 */
object PlayerTickers : Listener {
    private class Ticker(val period: Long, val action: (Player) -> Unit)

    private val tickers = CopyOnWriteArrayList<Ticker>()

    fun clear() {
        tickers.clear()
    }

    fun register(period: Long, action: (Player) -> Unit) {
        val ticker = Ticker(period, action)
        tickers += ticker

        if (!isFolia) {
            plugin.scheduler.global().runTimer(period, period) {
                for (player in Bukkit.getOnlinePlayers()) {
                    action(player)
                }
            }
            return
        }

        for (player in Bukkit.getOnlinePlayers()) {
            start(ticker, player)
        }
    }

    private fun start(ticker: Ticker, player: Player) {
        plugin.scheduler.on(player).runTimer(ticker.period, ticker.period) {
            ticker.action(player)
        }
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        if (!isFolia) {
            return
        }

        for (ticker in tickers) {
            start(ticker, event.player)
        }
    }
}
