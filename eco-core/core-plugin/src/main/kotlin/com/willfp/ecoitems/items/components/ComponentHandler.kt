package com.willfp.ecoitems.items.components

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.registry.KRegistrable
import com.willfp.eco.core.registry.Registry
import org.bukkit.inventory.ItemStack

abstract class ComponentHandler(override val id: String) : KRegistrable {
    abstract fun apply(item: ItemStack, config: Config)
}

object ComponentHandlers : Registry<ComponentHandler>() {
    init {
        register(ToolComponentHandler)
        register(FoodComponentHandler)
    }
}
