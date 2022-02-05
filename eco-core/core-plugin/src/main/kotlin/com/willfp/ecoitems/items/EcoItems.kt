package com.willfp.ecoitems.items

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.ImmutableList
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.config.updating.ConfigUpdater
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.recipe.Recipes
import com.willfp.eco.util.NumberUtils
import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.fuels.ConditionHasFuel
import com.willfp.libreforge.chains.EffectChains
import java.util.*

object EcoItems {
    /**
     * Registered items.
     */
    private val BY_ID: BiMap<String, EcoItem> = HashBiMap.create()

    val CONDITION_HAS_FUEL = ConditionHasFuel()

    /**
     * Get all registered [EcoItem]s.
     *
     * @return A list of all [EcoItem]s.
     */
    @JvmStatic
    fun values(): List<EcoItem> {
        return ImmutableList.copyOf(BY_ID.values)
    }

    /**
     * Get [EcoItem] matching id.
     *
     * @param name The id to search for.
     * @return The matching [EcoItem], or null if not found.
     */
    @JvmStatic
    fun getByID(name: String): EcoItem? {
        return BY_ID[name]
    }

    /**
     * Update all [EcoItem]s.
     *
     * @param plugin Instance of EcoItems.
     */
    @ConfigUpdater
    @JvmStatic
    fun update(plugin: EcoItemsPlugin) {
        plugin.itemsYml.getSubsections("chains").mapNotNull {
            EffectChains.compile(it, "Chains")
        }
        for (item in values()) {
            removeItem(item)
        }
        for (setConfig in plugin.itemsYml.getSubsections("items")) {
            addNewItem(EcoItem(setConfig, plugin))
        }
        for (recipeConfig in plugin.itemsYml.getSubsections("recipes")) {
            addNewRecipeFromConfig(recipeConfig)
        }
    }

    /**
     * Add new recipe to EcoItems.
     *
     * @param config The config for the recipe.
     */
    @JvmStatic
    fun addNewRecipeFromConfig(config: Config) {
        val result = Items.lookup(config.getString("result"))
        val item = result.item
        item.amount = config.getInt("recipeGiveAmount")
        Recipes.createAndRegisterRecipe(
            EcoItemsPlugin.instance,
            Objects.hash(config.getStrings("recipe")).toString(),
            item,
            config.getStrings("recipe"),
            config.getStringOrNull("permission")
        )
    }

    /**
     * Add new [EcoItem] to EcoItems.
     *
     * @param item The [EcoItem] to add.
     */
    @JvmStatic
    fun addNewItem(item: EcoItem) {
        BY_ID.remove(item.id)
        BY_ID[item.id] = item
    }

    /**
     * Remove [EcoItem] from EcoItems.
     *
     * @param item The [EcoItem] to remove.
     */
    @JvmStatic
    fun removeItem(item: EcoItem) {
        BY_ID.remove(item.id)
    }
}
