package com.willfp.ecoitems.items

import com.google.common.collect.ImmutableList
import com.willfp.eco.core.EcoPlugin
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

    /**
     * Get all registered [EcoItem]s.
     *
     * @return A list of all [EcoItem]s.
     */
    @JvmStatic
    fun values(): List<EcoItem> {
        return ImmutableList.copyOf(registry.values())
    }

    /**
     * Get [EcoItem] matching id.
     *
     * @param name The id to search for.
     * @return The matching [EcoItem], or null if not found.
     */
    @JvmStatic
    fun getByID(name: String): EcoItem? {
        return registry[name]
    }

    override fun clear(plugin: LibreforgePlugin) {
        registry.clear()
    }

    override fun acceptConfig(plugin: LibreforgePlugin, id: String, config: Config) {
        registry.register(EcoItem(id, config.separatorAmbivalent(), plugin as EcoPlugin))
    }
}
