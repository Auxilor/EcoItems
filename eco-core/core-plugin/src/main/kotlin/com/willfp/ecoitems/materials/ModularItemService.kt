package com.willfp.ecoitems.materials

import com.willfp.ecoitems.ext.builtFrom
import com.willfp.ecoitems.ext.madeFrom
import com.willfp.ecoitems.ext.modularMaterials
import com.willfp.ecoitems.ext.partMaterial
import com.willfp.ecoitems.items.EcoItem
import com.willfp.ecoitems.items.EcoItems
import com.willfp.ecoitems.items.ecoItem
import org.bukkit.inventory.ItemStack

/** The result of replacing one part of a modular item, and the part given back if any. */
data class PartSwap(
    val result: ItemStack,
    val returnedPart: ItemStack?,
    val cost: Int
)

/** Making parts, putting modular items together and swapping their parts. */
object ModularItemService {
    /**
     * The part item for a shape, used for textures and to give back a swapped-out part: the item whose
     * ID is the shape if it is a part of that shape, otherwise the first part item with the shape.
     */
    fun partItemForShape(shape: String): EcoItem? = EcoItems.getByID(shape)?.takeIf { it.part?.shape == shape }
        ?: EcoItems.values().firstOrNull { it.part?.shape == shape }

    /** A fresh part item of [item] made from [material]. */
    fun partOf(item: EcoItem, material: Material): ItemStack = item.itemStack.madeFrom(material)

    /** [result] made from the material of [addition], or null if it isn't a material or there's too little. */
    fun partResult(result: ItemStack, part: PartConfig, addition: ItemStack?): ItemStack? =
        Materials.forItem(addition)
            ?.takeIf { addition != null && addition.amount >= part.cost }
            ?.let { result.madeFrom(it) }

    /** [item] with the first slot of [part]'s shape replaced by its material, or null if nothing changes. */
    fun swap(item: ItemStack?, part: ItemStack?): PartSwap? {
        val modular = item?.ecoItem?.modular?.takeIf { it.isSwappable }
        val shape = part?.ecoItem?.part?.shape
        val newMaterial = part?.partMaterial

        if (modular == null || shape == null || newMaterial == null) {
            return null
        }

        val materials = item.modularMaterials
        val slotId = modular.slots.entries
            .firstOrNull { (slotId, slotShape) -> slotShape == shape && materials[slotId] != newMaterial }
            ?.key

        return slotId?.let { swappedSlot ->
            PartSwap(
                result = item.builtFrom(materials + (swappedSlot to newMaterial)).singleCopy(),
                returnedPart = returnedPart(modular, shape, materials[swappedSlot]),
                cost = modular.swapCost
            )
        }
    }

    /** [result] built from the parts in [matrix], each slot taking the first part with its shape. */
    fun assembledFrom(result: ItemStack, matrix: Array<ItemStack?>): ItemStack? {
        val modular = result.ecoItem?.modular ?: return null
        val parts = matrix.filterNotNull().filter { it.ecoItem?.part != null }.toMutableList()

        val materials = modular.slots.mapNotNull { (slotId, shape) ->
            parts.firstOrNull { it.ecoItem?.part?.shape == shape }
                ?.also { parts.remove(it) }
                ?.partMaterial
                ?.let { slotId to it }
        }.toMap()

        return if (materials.size == modular.slots.size) result.builtFrom(materials) else null
    }

    private fun returnedPart(modular: ModularConfig, shape: String, material: Material?): ItemStack? {
        val partItem = partItemForShape(shape)
        return if (modular.returnsSwappedPart && material != null && partItem != null) {
            partOf(partItem, material)
        } else {
            null
        }
    }

    private fun ItemStack.singleCopy(): ItemStack = clone().apply { amount = 1 }
}
