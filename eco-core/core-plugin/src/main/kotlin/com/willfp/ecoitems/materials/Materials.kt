package com.willfp.ecoitems.materials

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.registry.Registry
import com.willfp.ecoitems.premium.PremiumFeature
import com.willfp.ecoitems.premium.PremiumFeatures
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.ConfigCategory
import org.bukkit.inventory.ItemStack

object Materials : ConfigCategory("material", "materials") {
    private val registry = Registry<Material>()

    operator fun get(id: String?): Material? = id?.let { registry[it] }

    fun values(): List<Material> = registry.values().toList()

    /** The first material whose items match [item]. */
    fun forItem(item: ItemStack?): Material? = registry.values().firstOrNull { it.matches(item) }

    override fun clear(plugin: LibreforgePlugin) {
        registry.clear()
        MaterialHolders.clear()
    }

    override fun acceptConfig(plugin: LibreforgePlugin, id: String, config: Config) {
        if (PremiumFeatures.isLocked(PremiumFeature.MATERIALS, id)) {
            return
        }
        registry.register(Material(id, config))
    }
}
