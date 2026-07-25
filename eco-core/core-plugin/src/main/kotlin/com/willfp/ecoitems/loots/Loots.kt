package com.willfp.ecoitems.loots

import com.google.common.collect.ImmutableList
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.registry.Registry
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.ConfigCategory
import com.willfp.libreforge.separatorAmbivalent

object Loots : ConfigCategory("loot", "loots") {
    private val registry = Registry<Loot>()

    fun values(): List<Loot> {
        return ImmutableList.copyOf(registry.values())
    }

    override fun clear(plugin: LibreforgePlugin) {
        registry.clear()
    }

    override fun acceptConfig(plugin: LibreforgePlugin, id: String, config: Config) {
        registry.register(Loot(id, config.separatorAmbivalent()))
    }
}
