package com.willfp.ecoitems.items.components

import com.willfp.eco.core.config.interfaces.Config
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Consumable
import io.papermc.paper.datacomponent.item.consumable.ConsumeEffect
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect

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

        val eatSeconds = config.getDouble("eat-seconds").toFloat()

        val effects = mutableMapOf<PotionEffect, Float>()
        for (effectConfig in config.getSubsections("effects")) {
            val effect = Registry.POTION_EFFECT_TYPE.get(
                NamespacedKey.minecraft(effectConfig.getString("effect").lowercase())
            ) ?: continue

            val amplifier = effectConfig.getInt("level") - 1
            val duration = effectConfig.getInt("duration")

            val ambient = effectConfig.getBoolOrNull("ambient") ?: true
            val particles = effectConfig.getBoolOrNull("particles") ?: true
            val icon = effectConfig.getBoolOrNull("icon") ?: true

            val probability = (effectConfig.getDoubleOrNull("probability") ?: 100.0).toFloat() / 100f


            effects[PotionEffect(
                effect,
                duration,
                amplifier,
                ambient,
                particles,
                icon
            )] = probability

        }
        val consumeEffects = effects.map { ConsumeEffect.applyStatusEffects(listOf(it.key), it.value) }

        val consumable = Consumable.consumable()
            .consumeSeconds(eatSeconds)
            .addEffects(consumeEffects)

        meta.setFood(food)
        item.itemMeta = meta
        item.setData(DataComponentTypes.CONSUMABLE, consumable)
    }
}
