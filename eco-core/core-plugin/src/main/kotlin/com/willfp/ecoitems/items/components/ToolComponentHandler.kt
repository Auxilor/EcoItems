package com.willfp.ecoitems.items.components

import com.willfp.eco.core.config.interfaces.Config
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.inventory.ItemStack

object ToolComponentHandler : ComponentHandler("tool") {
    override fun apply(item: ItemStack, config: Config) {
        val meta = item.itemMeta ?: return

        val tool = meta.tool

        tool.defaultMiningSpeed = config.getDouble("mining-speed").toFloat()
        tool.damagePerBlock = config.getInt("damage-per-block")

        if (config.has("rules")) {
            for (rule in config.getSubsections("rules")) {
                val speed = rule.getDoubleOrNull("speed")?.toFloat()
                val drops = rule.getBoolOrNull("drops") ?: true // Leaving null causes weird behavior

                val materialIds = rule.getStrings("blocks")
                val materials = mutableSetOf<Material>()
                val tags = mutableSetOf<Tag<Material>>()

                for (id in materialIds) {
                    if (id.startsWith("#")) {
                        val tag = getTagByName(id.substring(1))
                        if (tag != null) {
                            tags.add(tag)
                        }
                    } else {
                        val material = Material.getMaterial(id.uppercase())
                        if (material != null) {
                            materials.add(material)
                        }
                    }
                }

                // Add rules for each tag
                for (tag in tags) {
                    tool.addRule(tag, speed, drops)
                }

                // Add rules for all materials
                if (materials.isNotEmpty()) {
                    tool.addRule(materials, speed, drops)
                }
            }
        }

        meta.setTool(tool)
        item.itemMeta = meta
    }

    private fun getTagByName(name: String): Tag<Material>? {
        try {
            @Suppress("UNCHECKED_CAST")
            return Tag::class.java.getDeclaredField(name.uppercase())
                .get(null) as Tag<Material>
        } catch (e: NoSuchFieldException) {
            return null
        }
    }
}
