package com.willfp.ecoitems.materials

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.placeholder.RegistrablePlaceholder
import com.willfp.eco.core.placeholder.context.PlaceholderContext
import com.willfp.ecoitems.BuildConfig
import com.willfp.ecoitems.ext.modularMaterials
import java.util.regex.Pattern

/**
 * %ecoitems_material_count_<material>%: how many slots of the modular item are made of that material.
 * Always 0 on the free version.
 */
class MaterialCountPlaceholder(private val plugin: EcoPlugin) : RegistrablePlaceholder {
    private val pattern = Pattern.compile("material_count_[a-z0-9_]+")

    override fun getValue(args: String, context: PlaceholderContext): String {
        if (BuildConfig.FREE_VERSION) {
            return "0"
        }
        val materialId = args.removePrefix("material_count_")
        val materials = context.itemStack?.modularMaterials.orEmpty()
        return materials.values.count { it.getID() == materialId }.toString()
    }

    override fun getPattern(): Pattern = pattern

    override fun getPlugin(): EcoPlugin = plugin
}
