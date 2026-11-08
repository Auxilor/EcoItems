package com.willfp.ecoitems.glyphs

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.configs.category.FoundConfig
import com.willfp.ecoitems.premium.PremiumFeature
import com.willfp.ecoitems.premium.PremiumFeatures
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.RegistrableCategory

object Glyphs : RegistrableCategory<Glyph>("glyph", "glyphs") {
    override fun beforeReload(plugin: LibreforgePlugin) {
        FoundConfig("items_gui", directory, plugin).copy()
    }

    override fun clear(plugin: LibreforgePlugin) {
        registry.clear()
    }

    override fun acceptConfig(plugin: LibreforgePlugin, id: String, config: Config) {
        PremiumFeatures.report(PremiumFeature.GLYPHS, id)
        registry.register(Glyph(id, config))
    }
}
