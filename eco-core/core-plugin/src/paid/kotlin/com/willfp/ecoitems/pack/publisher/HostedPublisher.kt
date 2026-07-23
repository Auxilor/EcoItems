package com.willfp.ecoitems.pack.publisher

import com.google.gson.JsonParser
import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.pack.BuiltPack
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Uploads the pack to a packhost instance (packs.auxilor.io by default).
 */
class HostedPublisher(
    private val plugin: EcoItemsPlugin,
    private val baseUrl: String
) : PackPublisher {
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    override fun publish(pack: BuiltPack): PublishedPack? {
        val request = HttpRequest.newBuilder(URI.create("$baseUrl/v1/packs"))
            .header("Content-Type", "application/zip")
            .header("User-Agent", "EcoItems/${plugin.description.version}")
            .timeout(Duration.ofSeconds(60))
            .POST(HttpRequest.BodyPublishers.ofFile(pack.file.toPath()))
            .build()

        return try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() != 200) {
                plugin.logger.severe("Pack upload to $baseUrl failed (${response.statusCode()}): ${response.body().take(200)}")
                return null
            }

            val body = JsonParser.parseString(response.body()).asJsonObject
            val url = body.get("url").asString
            val remoteSha1 = body.get("sha1").asString

            if (remoteSha1 != pack.sha1) {
                plugin.logger.severe("Pack upload to $baseUrl returned mismatched hash ($remoteSha1, expected ${pack.sha1})")
                return null
            }

            PublishedPack(url, pack)
        } catch (e: Exception) {
            plugin.logger.severe("Pack upload to $baseUrl failed: $e")
            null
        }
    }
}
