package com.willfp.ecoitems.materials

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.placeholder.RegistrablePlaceholder
import com.willfp.eco.core.placeholder.context.PlaceholderContext
import com.willfp.ecoitems.ext.modularMaterials
import java.util.regex.Pattern

/** %ecoitems_material_count_<material>%: how many slots of the modular item are made of that material. */
class MaterialCountPlaceholder(private val plugin: EcoPlugin) : RegistrablePlaceholder {
    private val pattern = Pattern.compile("material_count_[a-z0-9_]+")

    override fun getValue(args: String, context: PlaceholderContext): String {
        val materialId = args.removePrefix("material_count_")
        val materials = context.itemStack?.modularMaterials.orEmpty()
        return materials.values.count { it.getID() == materialId }.toString()
    }

    override fun getPattern(): Pattern = pattern

    override fun getPlugin(): EcoPlugin = plugin
}
