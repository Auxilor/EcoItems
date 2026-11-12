package com.willfp.ecoitems.items

import com.google.common.collect.ImmutableList
import com.willfp.ecoitems.premium.PremiumFeature
import com.willfp.ecoitems.premium.PremiumFeatures
import wiki.ecohub.config.Config
import wiki.ecohub.config.LegacyLocation
import wiki.ecohub.config.category.NativeConfigCategory
import wiki.ecohub.config.section.separatorAmbivalent
import wiki.ecohub.core.plugin.EcoPlugin
import wiki.ecohub.core.registry.Registry
import wiki.ecohub.modules.recipe.Recipes
import wiki.ecohub.modules.recipe.workstation.WorkstationRecipes

object EcoItems : NativeConfigCategory("items", "item") {
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

    override fun clear(plugin: EcoPlugin) {
        for (item in registry.values()) {
            item.craftingRecipe?.let { Recipes.unregister(it) }
        }
        registry.clear()
    }

    override fun beforeReload(plugin: EcoPlugin) {
        ItemTemplates.load()
    }

    override fun acceptConfig(plugin: EcoPlugin, id: String, config: Config) {
        if (registry.values().size >= PremiumFeatures.FREE_LIMIT &&
            PremiumFeatures.isLocked(PremiumFeature.ITEM_LIMIT, id)
        ) {
            return
        }
        registry.register(EcoItem(id, ItemTemplates.resolve(id, config).separatorAmbivalent()))
    }

    override fun afterReload(plugin: EcoPlugin) {
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
