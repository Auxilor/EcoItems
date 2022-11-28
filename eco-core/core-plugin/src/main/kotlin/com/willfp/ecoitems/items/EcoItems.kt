package com.willfp.ecoitems.items

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.ImmutableList
import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.config.ConfigType
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.config.readConfig
import com.willfp.eco.core.config.updating.ConfigUpdater
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.recipe.Recipes
import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.libreforge.separatorAmbivalent
import java.io.File
import java.util.Objects

object EcoItems {
    /** Registered items. */
    private val BY_ID: BiMap<String, EcoItem> = HashBiMap.create()

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
        BY_ID.clear()

        for ((id, config) in plugin.fetchConfigs("items")) {
            addNewItem(EcoItem(id, config.separatorAmbivalent(), plugin))
        }

        for ((id, config) in plugin.fetchConfigs("recipes", dontShare = true)) {
            addNewRecipeFromConfig(plugin, id, config.separatorAmbivalent())
        }

        val itemsYml = File(plugin.dataFolder, "items.yml").readConfig(ConfigType.YAML)

        // Legacy
        for (config in itemsYml.getSubsections("items")) {
            addNewItem(EcoItem(config.getString("id"), config.separatorAmbivalent(), plugin))
        }
        for (config in itemsYml.getSubsections("recipes")) {
            addNewRecipeFromConfig(
                plugin,
                Objects.hash(config.getStrings("recipe")).toString(),
                config.separatorAmbivalent()
            )
        }
    }

    /**
     * Add new recipe to EcoItems.
     *
     * @param config The config for the recipe.
     */
    @JvmStatic
    fun addNewRecipeFromConfig(plugin: EcoPlugin, id: String, config: Config) {
        val result = Items.lookup(config.getString("result"))
        val item = result.item
        if (config.has("recipe-give-amount")) {
            item.amount = config.getInt("recipe-give-amount") // Legacy
        }
        plugin.scheduler.run {
            Recipes.createAndRegisterRecipe(
                plugin,
                id,
                item,
                config.getStrings("recipe"),
                config.getStringOrNull("permission")
            )
        }
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
