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
    NONE;

    companion object {
        fun fromID(id: String): DeliveryMode? =
            entries.firstOrNull { it.name.equals(id.replace('-', '_'), ignoreCase = true) }
    }
}

class PackSettings(config: Config) {
    val enabled = config.getBool("enabled")
    val description: String = config.getString("description")

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

    val glyphsFormatChat = config.getBool("glyphs.format-chat")
    val glyphsFormatSigns = config.getBool("glyphs.format-signs")
    val glyphsTabComplete = config.getBool("glyphs.tab-complete")
}
