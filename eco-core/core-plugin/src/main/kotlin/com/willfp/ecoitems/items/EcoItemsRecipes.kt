package com.willfp.ecoitems.items

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.recipe.Recipes
import com.willfp.ecoitems.BuildConfig
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.ConfigCategory
import com.willfp.libreforge.loader.configs.LegacyLocation

object EcoItemsRecipes : ConfigCategory("recipe", "recipes") {
    private val registeredIds = mutableSetOf<String>()

    override val legacyLocation = LegacyLocation(
        "items.yml",
        "recipes"
    )

    override fun clear(plugin: LibreforgePlugin) {
        // Do nothing.
    }

    override fun acceptConfig(plugin: LibreforgePlugin, id: String, config: Config) {
        if (BuildConfig.FREE_VERSION && id !in registeredIds && registeredIds.size >= 10) {
            plugin.logger.warning("The free version of EcoItems only supports 10 recipes.")
            plugin.logger.warning("Purchase the full version of EcoItems to remove this restriction!")
            return
        }
        val result = Items.lookup(config.getString("result"))
        val item = result.item.clone()

        if (config.has("recipe-give-amount")) {
            item.amount = config.getInt("recipe-give-amount") // Legacy
        }

        registeredIds.add(id)

        plugin.scheduler.runTask {
            val recipeStrings = config.getStrings("recipe")
            if (recipeStrings.isEmpty()) return@runTask

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