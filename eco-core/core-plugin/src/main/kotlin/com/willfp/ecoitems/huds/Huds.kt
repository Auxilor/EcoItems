package com.willfp.ecoitems.huds

import com.willfp.ecoitems.premium.PremiumFeature
import com.willfp.ecoitems.premium.PremiumFeatures
import wiki.ecohub.config.Config
import wiki.ecohub.config.category.RegistrableCategory
import wiki.ecohub.core.plugin.EcoPlugin

object Huds : RegistrableCategory<Hud>("hud", "huds") {
    override fun clear(plugin: EcoPlugin) {
        registry.clear()
    }

    override fun acceptConfig(plugin: EcoPlugin, id: String, config: Config) {
        PremiumFeatures.report(PremiumFeature.HUDS, id)
        registry.register(Hud(id, config))
    }
}
