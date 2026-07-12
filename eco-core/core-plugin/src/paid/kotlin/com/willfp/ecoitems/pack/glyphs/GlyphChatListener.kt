package com.willfp.ecoitems.pack.glyphs

import com.willfp.ecoitems.EcoItemsPlugin
import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID

/** The chat/sign/tab-complete surfaces; the right chat listener per platform. */
object GlyphListeners {
    val isPaper: Boolean = runCatching {
        Class.forName("io.papermc.paper.event.player.AsyncChatEvent")
    }.isSuccess

    fun listeners(): List<Listener> = listOf(
        if (isPaper) PaperGlyphChatListener else SpigotGlyphChatListener,
        GlyphSignListener,
        GlyphTabCompletions
    )
}

object PaperGlyphChatListener : Listener {
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onChat(event: AsyncChatEvent) {
        if (!GlyphText.formatChat) {
            return
        }

        event.message(GlyphText.replaceComponent(event.message(), event.player))
    }
}

object SpigotGlyphChatListener : Listener {
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onChat(event: AsyncPlayerChatEvent) {
        if (!GlyphText.formatChat) {
            return
        }

        event.message = GlyphText.replaceLegacy(event.message, event.player, checkPermissions = true)
    }
}

object GlyphSignListener : Listener {
    @EventHandler(ignoreCancelled = true)
    fun onSignChange(event: SignChangeEvent) {
        if (!GlyphText.formatSigns) {
            return
        }

        for (index in 0..3) {
            val line = event.getLine(index) ?: continue
            @Suppress("DEPRECATION")
            event.setLine(index, GlyphText.replaceLegacy(line, event.player, checkPermissions = true))
        }
    }
}

/** Per-player chat completions for glyph placeholders (e.g. typing ":hea" completes ":heart:"). */
object GlyphTabCompletions : Listener {
    private val sent = mutableMapOf<UUID, List<String>>()

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        send(event.player)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        sent.remove(event.player.uniqueId)
    }

    fun refresh(plugin: EcoItemsPlugin) {
        plugin.scheduler.run {
            for (player in Bukkit.getOnlinePlayers()) {
                send(player)
            }
        }
    }

    private fun send(player: Player) {
        sent.remove(player.uniqueId)?.let { player.removeCustomChatCompletions(it) }

        if (!GlyphText.tabComplete) {
            return
        }

        val completions = GlyphText.assignments.values
            .filter { it.glyph.tabComplete && GlyphText.hasPermission(player, it.glyph) }
            .flatMap { it.glyph.placeholders }

        if (completions.isNotEmpty()) {
            player.addCustomChatCompletions(completions)
            sent[player.uniqueId] = completions
        }
    }
}
