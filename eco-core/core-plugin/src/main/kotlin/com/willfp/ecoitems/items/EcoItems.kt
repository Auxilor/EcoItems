package com.willfp.ecoitems.items

import com.google.common.collect.ImmutableList
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.recipe.workstation.WorkstationRecipes
import com.willfp.eco.core.registry.Registry
import com.willfp.ecoitems.plugin
import com.willfp.ecoitems.BuildConfig
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

    override fun beforeReload(plugin: LibreforgePlugin) {
        ItemTemplates.load()
    }

    override fun acceptConfig(plugin: LibreforgePlugin, id: String, config: Config) {
        if (BuildConfig.FREE_VERSION && registry.values().size >= 10) {
            plugin.logger.warning("The free version of EcoItems only supports 10 items.")
            plugin.logger.warning("Purchase the full version of EcoItems to remove this restriction!")
            return
        }
        registry.register(EcoItem(id, ItemTemplates.resolve(id, config).separatorAmbivalent()))
    }

    override fun afterReload(plugin: LibreforgePlugin) {
        // Recipes are registered after all items have been loaded so that
        // ingredients referring to other EcoItems can be resolved regardless
        // of load order.
        WorkstationRecipePermissions.clear()

        // Drop our old workstation recipes so deleted items don't leave one behind.
        // Scoped to our own namespace - the registry is shared with other plugins -
        // and taken from a real key so it always matches the ones we register.
        WorkstationRecipes.clear(plugin.createNamespacedKey("recipe").namespace)

        for (item in registry.values()) {
            item.registerRecipe()
        }
    }
}
