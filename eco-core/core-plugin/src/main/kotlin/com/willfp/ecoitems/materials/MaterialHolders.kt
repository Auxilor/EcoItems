package com.willfp.ecoitems.materials

import com.willfp.ecoitems.ext.modularMaterials
import com.willfp.ecoitems.items.EcoItem
import com.willfp.ecoitems.items.ecoItem
import com.willfp.ecoitems.plugin
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import wiki.ecohub.rules.condition.ConditionList
import wiki.ecohub.rules.effect.EffectList
import wiki.ecohub.rules.holder.Holder
import wiki.ecohub.rules.holder.slot.ItemHolderFinder
import wiki.ecohub.rules.holder.slot.SlotType
import java.util.concurrent.ConcurrentHashMap

/** One material in one slot of one modular item, active wherever that item is active. */
class MaterialHolder(
    val material: Material,
    val slotId: String,
    val item: EcoItem
) : Holder {
    override val id: NamespacedKey = plugin.createNamespacedKey("material_${material.getID()}_${slotId}_${item.id.key}")

    override val effects: EffectList = material.effectsInSlot(slotId)

    override val conditions: ConditionList = material.conditions
}

object MaterialHolders {
    private val holders = ConcurrentHashMap<String, MaterialHolder>()

    fun get(material: Material, slotId: String, item: EcoItem): MaterialHolder =
        holders.computeIfAbsent("${material.getID()}:$slotId:${item.id.key}") {
            MaterialHolder(material, slotId, item)
        }

    fun clear() {
        holders.clear()
    }
}

object MaterialFinder : ItemHolderFinder<MaterialHolder>() {
    override fun find(item: ItemStack): List<MaterialHolder> {
        val ecoItem = item.ecoItem ?: return emptyList()

        return item.modularMaterials.map { (slotId, material) -> MaterialHolders.get(material, slotId, ecoItem) }
    }

    override fun isValidInSlot(holder: MaterialHolder, slot: SlotType): Boolean = holder.item.slot.isOrContains(slot)
}
