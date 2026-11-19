package com.willfp.ecoitems.materials

import com.willfp.ecoitems.premium.PremiumFeature
import com.willfp.ecoitems.premium.PremiumFeatures
import org.bukkit.inventory.ItemStack
import wiki.ecohub.config.Config
import wiki.ecohub.config.category.NativeConfigCategory
import wiki.ecohub.core.plugin.EcoPlugin
import wiki.ecohub.core.registry.Registry

object Materials : NativeConfigCategory("materials", "material") {
    private val registry = Registry<Material>()

    operator fun get(id: String?): Material? = id?.let { registry[it] }

    fun values(): List<Material> = registry.values().toList()

    /** The first material whose items match [item]. */
    fun forItem(item: ItemStack?): Material? = registry.values().firstOrNull { it.matches(item) }

    override fun clear(plugin: EcoPlugin) {
        registry.clear()
        MaterialHolders.clear()
    }

    override fun acceptConfig(plugin: EcoPlugin, id: String, config: Config) {
        if (PremiumFeatures.isLocked(PremiumFeature.MATERIALS, id)) {
            return
        }
        registry.register(Material(id, config))
    }
}
