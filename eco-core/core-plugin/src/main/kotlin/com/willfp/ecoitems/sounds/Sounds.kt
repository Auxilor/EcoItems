package com.willfp.ecoitems.sounds

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecoitems.premium.PremiumFeature
import com.willfp.ecoitems.premium.PremiumFeatures
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.RegistrableCategory

object Sounds : RegistrableCategory<Sound>("sound", "sounds") {
    override fun clear(plugin: LibreforgePlugin) {
        registry.clear()
    }

    override fun acceptConfig(plugin: LibreforgePlugin, id: String, config: Config) {
        PremiumFeatures.report(PremiumFeature.SOUNDS, id)
        registry.register(Sound(id, config))
    }
}
