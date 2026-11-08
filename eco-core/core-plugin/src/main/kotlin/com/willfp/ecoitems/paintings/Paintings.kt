package com.willfp.ecoitems.paintings

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecoitems.premium.PremiumFeature
import com.willfp.ecoitems.premium.PremiumFeatures
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.RegistrableCategory

object Paintings : RegistrableCategory<Painting>("painting", "paintings") {
    override fun clear(plugin: LibreforgePlugin) {
        registry.clear()
    }

    override fun acceptConfig(plugin: LibreforgePlugin, id: String, config: Config) {
        PremiumFeatures.report(PremiumFeature.PAINTINGS, id)
        registry.register(Painting(id, config))
    }
}
