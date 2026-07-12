package com.willfp.ecoitems.pack.delivery

import com.willfp.eco.util.formatEco
import com.willfp.ecoitems.pack.PackSettings
import com.willfp.ecoitems.pack.publisher.PublishedPack
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * Sends the published pack to players.
 */
object PackDelivery {
    // A fixed id means re-sends replace the previous pack on the client
    // instead of stacking a second one.
    private val PACK_ID: UUID = UUID.nameUUIDFromBytes("com.willfp.ecoitems:pack".toByteArray())

    var current: PublishedPack? = null
        private set

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
            send(player)
        }
    }
}
