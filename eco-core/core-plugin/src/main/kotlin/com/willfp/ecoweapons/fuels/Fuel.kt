package com.willfp.ecoweapons.fuels

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.display.Display
import com.willfp.eco.core.items.CustomItem
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.core.recipe.Recipes
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.Objects

class Fuel(
    private val config: Config,
    private val plugin: EcoPlugin
) {
    val id = config.getString("name")

    val itemStack: ItemStack = run {
        val itemConfig = config.getSubsection("item")
        ItemStackBuilder(Items.lookup(itemConfig.getString("item")).item).apply {
            setDisplayName(itemConfig.getFormattedString("displayName"))
            addLoreLines(
                itemConfig.getFormattedStrings("lore").map { "${Display.PREFIX}$it" })
            writeMetaKey(
                this@Fuel.plugin.namespacedKeyFactory.create("fuel"),
                PersistentDataType.STRING,
                this@Fuel.id
            )
        }.build()
    }

    val customItem = CustomItem(
        plugin.namespacedKeyFactory.create(id),
        { test -> FuelUtils.getFuelFromItem(test) == this },
        itemStack
    ).apply { register() }

    val craftingRecipe = if (config.getBool("item.craftable")) {
        Recipes.createAndRegisterRecipe(
            plugin,
            id,
            itemStack.clone().apply { amount = config.getInt("item.recipeGiveAmount") },
            config.getStrings("item.recipe")
        )
    } else null

    override fun equals(other: Any?): Boolean {
        if (other !is Fuel) {
            return false
        }

        return this.id == other.id
    }

    override fun hashCode(): Int {
        return Objects.hash(this.id)
    }

    override fun toString(): String {
        return "Weapon{$id}"
    }
}