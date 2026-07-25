package com.willfp.ecoitems.pack.publisher

import com.sun.net.httpserver.HttpServer
import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.pack.BuiltPack
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Serves the pack over HTTP from the Minecraft server itself.
 *
 * The current and previous packs are both kept available so that players
 * mid-download during a reload don't hit a missing file.
 */
class SelfHostedPublisher(
    private val plugin: EcoItemsPlugin,
    val bind: String,
    val port: Int,
    val publicUrl: String
) : PackPublisher {
    private val packs = ConcurrentHashMap<String, ByteArray>()
    private val history = ArrayDeque<String>()
    private var server: HttpServer? = null

    override fun publish(pack: BuiltPack): PublishedPack? {
        if (publicUrl.isBlank()) {
            plugin.logger.severe("Self-hosted pack delivery requires delivery.self-hosted.public-url to be set in pack.yml")
            return null
        }

        synchronized(history) {
            if (!packs.containsKey(pack.sha1)) {
                packs[pack.sha1] = pack.file.readBytes()
                history.addLast(pack.sha1)
            }
            while (history.size > 2) {
                packs.remove(history.removeFirst())
            }
        }

        if (server == null && !start()) {
            return null
        }

        return PublishedPack("$publicUrl/packs/${pack.sha1}.zip", pack)
    }

    private fun start(): Boolean = try {
        val created = HttpServer.create(InetSocketAddress(bind, port), 0)
        created.executor = Executors.newFixedThreadPool(2)
        created.createContext("/packs/") { exchange ->
            val sha1 = exchange.requestURI.path
                .removePrefix("/packs/")
                .removeSuffix(".zip")

            val bytes = packs[sha1]
            exchange.use {
                if (bytes == null) {
                    exchange.sendResponseHeaders(404, -1)
                } else {
                    exchange.responseHeaders.add("Content-Type", "application/zip")
                    exchange.sendResponseHeaders(200, bytes.size.toLong())
                    exchange.responseBody.write(bytes)
                }
            }
        }
        created.start()
        server = created
        true
    } catch (e: Exception) {
        plugin.logger.severe("Could not start the pack server on $bind:$port: $e")
        false
    }

    override fun shutdown() {
        server?.stop(0)
        server = null
    }
}
