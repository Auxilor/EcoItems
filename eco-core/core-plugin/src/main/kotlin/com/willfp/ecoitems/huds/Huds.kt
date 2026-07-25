package com.willfp.ecoitems.huds

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecoitems.BuildConfig
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.RegistrableCategory

object Huds : RegistrableCategory<Hud>("hud", "huds") {
    override fun clear(plugin: LibreforgePlugin) {
        registry.clear()
    }

    override fun acceptConfig(plugin: LibreforgePlugin, id: String, config: Config) {
        registry.register(Hud(id, config))
    }

    override fun afterReload(plugin: LibreforgePlugin) {
        if (BuildConfig.FREE_VERSION && registry.values().isNotEmpty()) {
            plugin.logger.warning("${registry.values().size} HUDs are configured, but HUDs require the paid version of EcoItems.")
            plugin.logger.warning("Purchase the full version of EcoItems to use HUDs!")
        }
    }
}
