package com.willfp.ecoitems.pack

import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.items.EcoItems
import com.willfp.ecoitems.pack.delivery.PackDelivery
import com.willfp.ecoitems.pack.delivery.PackListener
import com.willfp.ecoitems.pack.publisher.ExternalPublisher
import com.willfp.ecoitems.pack.publisher.HostedPublisher
import com.willfp.ecoitems.pack.publisher.PackPublisher
import com.willfp.ecoitems.pack.publisher.SelfHostedPublisher
import org.bukkit.event.Listener

object EcoItemsPackFeature : PackFeature {
    private var packYml: PackYml? = null
    private var publisher: PackPublisher? = null

    override fun listeners(plugin: EcoItemsPlugin): List<Listener> {
        return listOf(PackListener)
    }

    override fun handleReload(plugin: EcoItemsPlugin) {
        val config = packYml ?: PackYml(plugin).also { packYml = it }
        val settings = PackSettings(config)

        if (!settings.enabled) {
            shutdownPublisher()
            PackDelivery.clear()
            return
        }

        val assets = EcoItems.values().mapNotNull { ItemPackAsset.fromItem(it) }
        val pack = PackBuilder.build(plugin, settings, assets)

        val published = resolvePublisher(plugin, settings)?.publish(pack)
        PackDelivery.update(published, settings)

        if (published != null && settings.sendOnReload) {
            PackDelivery.sendAll()
        }
    }

    override fun handleDisable(plugin: EcoItemsPlugin) {
        shutdownPublisher()
    }

    private fun resolvePublisher(plugin: EcoItemsPlugin, settings: PackSettings): PackPublisher? {
        // The self-hosted server is reused across reloads (unless rebound) so
        // in-flight downloads survive; everything else is stateless.
        val current = publisher
        if (current is SelfHostedPublisher &&
            settings.mode == DeliveryMode.SELF_HOSTED &&
            current.bind == settings.selfHostedBind &&
            current.port == settings.selfHostedPort &&
            current.publicUrl == settings.selfHostedPublicUrl
        ) {
            return current
        }

        shutdownPublisher()

        val created = when (settings.mode) {
            DeliveryMode.HOSTED -> HostedPublisher(plugin, settings.hostedUrl)
            DeliveryMode.SELF_HOSTED -> SelfHostedPublisher(
                plugin,
                settings.selfHostedBind,
                settings.selfHostedPort,
                settings.selfHostedPublicUrl
            )
            DeliveryMode.EXTERNAL -> ExternalPublisher(plugin, settings.externalDirectory, settings.externalUrl)
            DeliveryMode.NONE -> null
        }

        publisher = created
        return created
    }

    private fun shutdownPublisher() {
        publisher?.shutdown()
        publisher = null
    }
}
