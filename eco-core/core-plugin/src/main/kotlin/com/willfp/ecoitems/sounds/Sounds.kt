package com.willfp.ecoitems.sounds

import com.willfp.ecoitems.premium.PremiumFeature
import com.willfp.ecoitems.premium.PremiumFeatures
import wiki.ecohub.config.Config
import wiki.ecohub.config.category.RegistrableCategory
import wiki.ecohub.core.plugin.EcoPlugin

object Sounds : RegistrableCategory<Sound>("sound", "sounds") {
    override fun clear(plugin: EcoPlugin) {
        registry.clear()
    }

    override fun acceptConfig(plugin: EcoPlugin, id: String, config: Config) {
        PremiumFeatures.report(PremiumFeature.SOUNDS, id)
        registry.register(Sound(id, config))
    }
}
