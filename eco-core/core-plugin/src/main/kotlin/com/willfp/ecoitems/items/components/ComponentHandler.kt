package com.willfp.ecoitems.items.components

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.registry.KRegistrable
import com.willfp.eco.core.registry.Registry
import com.willfp.ecoitems.compat.ifModern
import org.bukkit.inventory.ItemStack

abstract class ComponentHandler(override val id: String) : KRegistrable {
    abstract fun apply(item: ItemStack, config: Config)
}

object ComponentHandlers : Registry<ComponentHandler>() {
    init {
        ifModern {
            register(loadProxy<ToolComponentHandler>())
            register(loadProxy<FoodComponentHandler>())
        }
    }
}
