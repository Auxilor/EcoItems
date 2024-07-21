package com.willfp.ecoitems.compat.modern.components

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecoitems.items.components.ToolComponentHandler
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.inventory.ItemStack

@Suppress("UnstableApiUsage")
class ToolComponentHandlerImpl : ToolComponentHandler() {
    override fun apply(item: ItemStack, config: Config) {
        val meta = item.itemMeta ?: return

        val tool = meta.tool

        tool.defaultMiningSpeed = config.getDouble("mining-speed").toFloat()
        tool.damagePerBlock = config.getInt("damage-per-block")

        if (config.has("rules")) {
            tool.rules = mutableListOf()

            for (rule in config.getSubsections("rules")) {
                val speed = config.getDoubleOrNull("speed")?.toFloat()
                val drops = config.getBoolOrNull("drops")

                val materialIds = config.getStrings("blocks")
                val materials = mutableSetOf<Material>()

                for (id in materialIds) {
                    if (id.startsWith("#")) {
                        val tag = getTagByName(id.substring(1))
                        if (tag != null) {
                            materials.addAll(tag.values)
                        }
                    } else {
                        val material = Material.getMaterial(id.uppercase())
                        if (material != null) {
                            materials.add(material)
                        }
                    }
                }

                tool.addRule(materials, speed, drops)
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
