package com.willfp.ecoitems.paintings

import com.willfp.ecoitems.premium.PremiumFeature
import com.willfp.ecoitems.premium.PremiumFeatures
import wiki.ecohub.config.Config
import wiki.ecohub.config.category.RegistrableCategory
import wiki.ecohub.core.plugin.EcoPlugin

object Paintings : RegistrableCategory<Painting>("painting", "paintings") {
    override fun clear(plugin: EcoPlugin) {
        registry.clear()
    }

    override fun acceptConfig(plugin: EcoPlugin, id: String, config: Config) {
        PremiumFeatures.report(PremiumFeature.PAINTINGS, id)
        registry.register(Painting(id, config))
    }
}
