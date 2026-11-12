package com.willfp.ecoitems.glyphs

import com.willfp.ecoitems.premium.PremiumFeature
import com.willfp.ecoitems.premium.PremiumFeatures
import wiki.ecohub.config.Config
import wiki.ecohub.config.category.FoundConfig
import wiki.ecohub.config.category.RegistrableCategory
import wiki.ecohub.core.plugin.EcoPlugin

object Glyphs : RegistrableCategory<Glyph>("glyph", "glyphs") {
    override fun beforeReload(plugin: EcoPlugin) {
        FoundConfig("items_gui", directory, plugin).copy()
    }

    override fun clear(plugin: EcoPlugin) {
        registry.clear()
    }

    override fun acceptConfig(plugin: EcoPlugin, id: String, config: Config) {
        PremiumFeatures.report(PremiumFeature.GLYPHS, id)
        registry.register(Glyph(id, config))
    }
}
