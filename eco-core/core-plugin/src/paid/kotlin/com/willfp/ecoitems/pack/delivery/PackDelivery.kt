package com.willfp.ecoitems.pack.delivery

import com.willfp.ecoitems.pack.PackSettings
import com.willfp.ecoitems.pack.publisher.PublishedPack
import com.willfp.ecoitems.plugin
import com.willfp.ecoitems.util.runFor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import wiki.ecohub.ui.text.formatEco
import java.util.UUID

/**
 * Sends the published pack to players.
 */
object PackDelivery {
    // A fixed id means re-sends replace the previous pack on the client
    // instead of stacking a second one.
    private val PACK_ID: UUID = UUID.nameUUIDFromBytes("com.willfp.ecoitems:pack".toByteArray())

    @Volatile
    var current: PublishedPack? = null
        private set

    @Volatile
    var settings: PackSettings? = null
        private set

    fun update(pack: PublishedPack?, settings: PackSettings) {
        this.current = pack
        this.settings = settings
    }

    fun clear() {
        this.current = null
    }

    fun send(player: Player) {
        val pack = current ?: return
        val settings = settings ?: return

        @Suppress("DEPRECATION")
        player.setResourcePack(
            PACK_ID,
            pack.url,
            pack.sha1Bytes,
            settings.prompt.formatEco(player),
            settings.required
        )
    }

    fun sendAll() {
        for (player in Bukkit.getOnlinePlayers()) {
            plugin.runFor(player) { send(player) }
        }
    }
}
