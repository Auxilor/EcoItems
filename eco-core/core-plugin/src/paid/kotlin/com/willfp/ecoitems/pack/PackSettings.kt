package com.willfp.ecoitems.pack

import com.willfp.eco.core.config.BaseConfig
import com.willfp.eco.core.config.ConfigType
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecoitems.EcoItemsPlugin

class PackYml(plugin: EcoItemsPlugin) : BaseConfig("pack", plugin, true, ConfigType.YAML)

enum class DeliveryMode {
    HOSTED,
    SELF_HOSTED,
    EXTERNAL,
    S3,
    NONE;

    companion object {
        fun fromID(id: String): DeliveryMode? =
            entries.firstOrNull { it.name.equals(id.replace('-', '_'), ignoreCase = true) }
    }
}

class PackSettings(config: Config) {
    val enabled = config.getBool("enabled")
    val description: String = config.getString("description")
    val minifyJson = config.getBoolOrNull("minify-json") ?: true
    val obfuscate = config.getBool("obfuscation")

    val hideScoreboardBackground = config.getBool("interface.hide-scoreboard-background")
    val hideTablistBackground = config.getBool("interface.hide-tablist-background")

    val mode = DeliveryMode.fromID(config.getString("delivery.mode")) ?: DeliveryMode.NONE
    val prompt: String = config.getString("delivery.prompt")
    val required = config.getBool("delivery.required")
    val kickOnDecline = config.getBool("delivery.kick-on-decline")
    val sendOnJoin = config.getBool("delivery.send-on-join")
    val joinDelayTicks = config.getInt("delivery.join-delay-ticks")
    val sendOnReload = config.getBool("delivery.send-on-reload")

    val hostedUrl: String = config.getString("delivery.hosted.url").removeSuffix("/")

    val selfHostedBind: String = config.getString("delivery.self-hosted.bind")
    val selfHostedPort = config.getInt("delivery.self-hosted.port")
    val selfHostedPublicUrl: String = config.getString("delivery.self-hosted.public-url").removeSuffix("/")

    val externalDirectory: String = config.getString("delivery.external.directory")
    val externalUrl: String = config.getString("delivery.external.url")

    val s3Endpoint: String = config.getString("delivery.s3.endpoint").removeSuffix("/")
    val s3Region: String = config.getString("delivery.s3.region").ifEmpty { "us-east-1" }
    val s3Bucket: String = config.getString("delivery.s3.bucket")
    val s3AccessKey: String = config.getString("delivery.s3.access-key")
    val s3SecretKey: String = config.getString("delivery.s3.secret-key")
    val s3PublicUrl: String = config.getString("delivery.s3.public-url").removeSuffix("/")
    val s3PublicRead = config.getBoolOrNull("delivery.s3.public-read") ?: true
    val s3PathStyle = config.getBoolOrNull("delivery.s3.path-style") ?: true

    val glyphsFormatChat = config.getBool("glyphs.format-chat")
    val glyphsFormatSigns = config.getBool("glyphs.format-signs")
    val glyphsTabComplete = config.getBool("glyphs.tab-complete")

    val customBlockSounds = config.getBool("blocks.custom-sounds")

    // Missing keys default to enabled so existing installs keep behaving.
    val defaultAssets = config.getBoolOrNull("compatibility.default-assets") ?: true
    val glyphShaders = config.getBoolOrNull("compatibility.glyph-shaders") ?: true
}
