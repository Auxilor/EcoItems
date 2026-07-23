package com.willfp.ecoitems.items

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.recipe.Recipes
import com.willfp.ecoitems.BuildConfig
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.ConfigCategory
import com.willfp.libreforge.loader.configs.LegacyLocation

/**
 * Deprecated: standalone recipes in the recipes/ folder, which craft any item rather
 * than an EcoItem. Kept loading so existing setups keep working, but no longer
 * documented and deliberately silent - it never logs, so a server that isn't using
 * it never hears about it.
 *
 * Give the item its own config with a recipe instead; see WorkstationRecipeLoader.
 */
object EcoItemsRecipes : ConfigCategory("recipe", "recipes") {
    private val registeredIds = mutableSetOf<String>()

    val size: Int get() = registeredIds.size

    override val legacyLocation = LegacyLocation(
        "items.yml",
        "recipes"
    )

    override fun clear(plugin: LibreforgePlugin) {
        // Do nothing.
    }

    override fun acceptConfig(plugin: LibreforgePlugin, id: String, config: Config) {
        if (BuildConfig.FREE_VERSION && id !in registeredIds && registeredIds.size >= 10) {
            return
        }
        val result = Items.lookup(config.getString("result"))
        val item = result.item.clone()

        if (config.has("recipe-give-amount")) {
            item.amount = config.getInt("recipe-give-amount") // Legacy
        }

        plugin.scheduler.run {
            registeredIds.add(id)
            val recipeStrings = config.getStrings("recipe")
            if (recipeStrings.isEmpty()) return@run

            if (item.type.isAir) {
                return@run
            }

            Recipes.createAndRegisterRecipe(
                plugin,
                id,
                item,
                recipeStrings,
                config.getStringOrNull("permission"),
                config.getBool("shapeless")
            )
        }
    }
}