package com.willfp.ecoitems.items

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.recipe.Recipes
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.ConfigCategory
import com.willfp.libreforge.loader.configs.LegacyLocation

object EcoItemsRecipes : ConfigCategory("recipe", "recipes") {
    private var recipes: Int = 0
    override val legacyLocation = LegacyLocation(
        "items.yml",
        "recipes"
    )

    override fun clear(plugin: LibreforgePlugin) {
        recipes = 0
    }

    override fun acceptConfig(plugin: LibreforgePlugin, id: String, config: Config) {
        if (recipes >= 10) {
            plugin.logger.warning("Recipe $id was not registered.")
            plugin.logger.warning("The free version of EcoItems only supports 10 custom recipes.")
            plugin.logger.warning("Purchase the full version of EcoItems to remove this restriction!")
            return
        }

        val result = Items.lookup(config.getString("result"))
        val item = result.item.clone()

        if (config.has("recipe-give-amount")) {
            item.amount = config.getInt("recipe-give-amount") // Legacy
        }

        plugin.scheduler.run {
            val recipeStrings = config.getStrings("recipe")
            if (recipeStrings.isEmpty()) return@run

            Recipes.createAndRegisterRecipe(
                plugin,
                id,
                item,
                recipeStrings,
                config.getStringOrNull("permission"),
                config.getBool("shapeless")
            )
        }

        recipes
    }
}