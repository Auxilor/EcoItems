package com.willfp.ecoitems.paintings

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecoitems.BuildConfig
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.RegistrableCategory

object Paintings : RegistrableCategory<Painting>("painting", "paintings") {
    override fun clear(plugin: LibreforgePlugin) {
        registry.clear()
    }

    override fun acceptConfig(plugin: LibreforgePlugin, id: String, config: Config) {
        registry.register(Painting(id, config))
    }

    override fun afterReload(plugin: LibreforgePlugin) {
        if (BuildConfig.FREE_VERSION && registry.values().isNotEmpty()) {
            plugin.logger.warning("${registry.values().size} paintings are configured, but paintings require the paid version of EcoItems.")
            plugin.logger.warning("Purchase the full version of EcoItems to use custom paintings!")
        }
    }
}
