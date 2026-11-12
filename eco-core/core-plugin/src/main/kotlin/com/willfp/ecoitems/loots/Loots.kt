package com.willfp.ecoitems.loots

import com.google.common.collect.ImmutableList
import wiki.ecohub.config.Config
import wiki.ecohub.config.category.NativeConfigCategory
import wiki.ecohub.config.section.separatorAmbivalent
import wiki.ecohub.core.plugin.EcoPlugin
import wiki.ecohub.core.registry.Registry

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
