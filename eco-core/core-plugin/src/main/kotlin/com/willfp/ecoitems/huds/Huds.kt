package com.willfp.ecoitems.huds

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecoitems.premium.PremiumFeature
import com.willfp.ecoitems.premium.PremiumFeatures
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.RegistrableCategory

object Huds : RegistrableCategory<Hud>("hud", "huds") {
    override fun clear(plugin: LibreforgePlugin) {
        registry.clear()
    }

    override fun acceptConfig(plugin: LibreforgePlugin, id: String, config: Config) {
        PremiumFeatures.report(PremiumFeature.HUDS, id)
        registry.register(Hud(id, config))
    }
}
