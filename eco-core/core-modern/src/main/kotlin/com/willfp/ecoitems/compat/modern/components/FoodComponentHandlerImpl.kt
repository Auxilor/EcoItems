package com.willfp.ecoitems.compat.modern.components

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecoitems.items.components.FoodComponentHandler
import com.willfp.ecoitems.items.components.ToolComponentHandler
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.Tag
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

@Suppress("UnstableApiUsage")
class FoodComponentHandlerImpl : FoodComponentHandler() {
    override fun apply(item: ItemStack, config: Config) {
        val meta = item.itemMeta ?: return

        val food = meta.food
        food.nutrition = config.getInt("nutrition")
        food.saturation = config.getDouble("saturation").toFloat()
        food.eatSeconds = config.getDouble("eat-seconds").toFloat()

        if (config.has("can-always-eat")) {
            food.setCanAlwaysEat(config.getBool("can-always-eat"))
        }

        food.effects = mutableListOf()

        for (effectConfig in config.getSubsections("effects")) {
            val effect = Registry.POTION_EFFECT_TYPE.get(
                NamespacedKey.minecraft(effectConfig.getString("effect").lowercase())
            ) ?: continue

            val amplifier = effectConfig.getInt("level") - 1
            val duration = effectConfig.getInt("duration")

            val ambient = effectConfig.getBoolOrNull("ambient") ?: true
            val particles = effectConfig.getBoolOrNull("particles") ?: true
            val icon = effectConfig.getBoolOrNull("icon") ?: true

            val probability = effectConfig.getDouble("probability").toFloat() / 100f

            food.addEffect(
                PotionEffect(
                    effect,
                    duration,
                    amplifier,
                    ambient,
                    particles,
                    icon
                ),
                probability
            )
        }

        meta.setFood(food)
        item.itemMeta = meta
    }
}
