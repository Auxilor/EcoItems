package com.willfp.ecoitems.items.components

import com.willfp.eco.core.config.interfaces.Config
import org.bukkit.inventory.ItemStack

@Suppress("UnstableApiUsage")
object FoodComponentHandler : ComponentHandler("food") {
    override fun apply(item: ItemStack, config: Config) {
        val meta = item.itemMeta ?: return

        val food = meta.food
        food.nutrition = config.getInt("nutrition")
        food.saturation = config.getDouble("saturation").toFloat()

        if (config.has("can-always-eat")) {
            food.setCanAlwaysEat(config.getBool("can-always-eat"))
        }

        meta.setFood(food)
        item.itemMeta = meta
    }
}
