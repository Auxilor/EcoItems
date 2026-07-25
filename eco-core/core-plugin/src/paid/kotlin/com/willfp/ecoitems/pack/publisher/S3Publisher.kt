package com.willfp.ecoitems.pack.publisher

import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.pack.BuiltPack
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Uploads the pack to S3-compatible object storage (AWS, R2, MinIO, ...)
 * with a hand-rolled SigV4 PUT - no SDK. Objects are keyed by content hash,
 * so re-uploads of an unchanged pack overwrite themselves harmlessly.
 */
class S3Publisher(
    private val plugin: EcoItemsPlugin,
    private val endpoint: String,
    private val region: String,
    private val bucket: String,
    private val accessKey: String,
    private val secretKey: String,
    private val publicUrl: String,
    private val publicRead: Boolean,
    private val pathStyle: Boolean
) : PackPublisher {
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    override fun publish(pack: BuiltPack): PublishedPack? {
        if (bucket.isEmpty() || accessKey.isEmpty() || secretKey.isEmpty()) {
            plugin.logger.severe("S3 delivery needs delivery.s3 bucket, access-key, and secret-key in pack.yml")
            return null
        }

        val key = "${pack.sha1}.zip"
        val host = URI.create(endpoint).let { uri ->
            uri.host + if (uri.port != -1) ":${uri.port}" else ""
        }
        val scheme = URI.create(endpoint).scheme ?: "https"

        val (requestHost, canonicalUri) = if (pathStyle) {
            host to "/$bucket/$key"
        } else {
            "$bucket.$host" to "/$key"
        }

        val payload = pack.file.readBytes()
        val payloadHash = hex(sha256(payload))

        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val amzDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"))
        val dateStamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"))

        val headers = sortedMapOf(
            "host" to requestHost,
            "x-amz-content-sha256" to payloadHash,
            "x-amz-date" to amzDate
        )
        if (publicRead) {
            headers["x-amz-acl"] = "public-read"
        }

        val signedHeaders = headers.keys.joinToString(";")
        val canonicalRequest = buildString {
            append("PUT\n").append(canonicalUri).append("\n\n")
            for ((name, value) in headers) {
                append(name).append(':').append(value).append('\n')
            }
            append('\n').append(signedHeaders).append('\n').append(payloadHash)
        }

        val scope = "$dateStamp/$region/s3/aws4_request"
        val stringToSign = "AWS4-HMAC-SHA256\n$amzDate\n$scope\n${hex(sha256(canonicalRequest.encodeToByteArray()))}"

        var signingKey = hmac("AWS4$secretKey".encodeToByteArray(), dateStamp)
        signingKey = hmac(signingKey, region)
        signingKey = hmac(signingKey, "s3")
        signingKey = hmac(signingKey, "aws4_request")
        val signature = hex(hmac(signingKey, stringToSign))

        val request = HttpRequest.newBuilder(URI.create("$scheme://$requestHost$canonicalUri"))
            .header(
                "Authorization",
                "AWS4-HMAC-SHA256 Credential=$accessKey/$scope, SignedHeaders=$signedHeaders, Signature=$signature"
            )
            .header("x-amz-content-sha256", payloadHash)
            .header("x-amz-date", amzDate)
            .apply { if (publicRead) header("x-amz-acl", "public-read") }
            .timeout(Duration.ofSeconds(60))
            .PUT(HttpRequest.BodyPublishers.ofByteArray(payload))
            .build()

        return try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) {
                plugin.logger.severe(
                    "S3 upload to $requestHost failed (${response.statusCode()}): ${response.body().take(200)}"
                )
                return null
            }

            val base = publicUrl.ifEmpty {
                if (pathStyle) "$endpoint/$bucket" else "$scheme://$bucket.$host"
            }.removeSuffix("/")

            PublishedPack("$base/$key", pack)
        } catch (e: Exception) {
            plugin.logger.severe("S3 upload to $requestHost failed: $e")
            null
        }
    }

    private fun sha256(bytes: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(bytes)

    private fun hmac(key: ByteArray, data: String): ByteArray =
        Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec(key, "HmacSHA256"))
        }.doFinal(data.encodeToByteArray())

    private fun hex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }
}
