package com.willfp.ecoitems.items

import com.google.common.collect.ImmutableList
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.registry.Registry
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.ConfigCategory
import com.willfp.libreforge.loader.configs.LegacyLocation
import com.willfp.libreforge.separatorAmbivalent

object EcoItems : ConfigCategory("item", "items") {
    /** Registered items. */
    private val registry = Registry<EcoItem>()

    override val legacyLocation = LegacyLocation(
        "items.yml",
        "items"
    )

    fun getByID(id: String?): EcoItem? {
        if (id == null) {
            return null
        }

        return registry[id]
    }

    fun values(): List<EcoItem> {
        return ImmutableList.copyOf(registry.values())
    }

    override fun clear(plugin: LibreforgePlugin) {
        registry.clear()
    }

    override fun acceptConfig(plugin: LibreforgePlugin, id: String, config: Config) {
        if (registry.values().size >= 10) {
            plugin.logger.warning("The free version of EcoItems only supports 10 custom items.")
            plugin.logger.warning("Purchase the full version of EcoItems to remove this restriction!")
            return
        }

        registry.register(EcoItem(id, config.separatorAmbivalent()))
    }
}
