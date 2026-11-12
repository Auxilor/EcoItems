package com.willfp.ecoitems.materials

import com.willfp.ecoitems.ext.MODULAR_MATERIALS_KEY
import com.willfp.ecoitems.ext.PART_MATERIAL_KEY
import com.willfp.ecoitems.ext.materialArgs
import com.willfp.ecoitems.ext.modularMaterials
import com.willfp.ecoitems.ext.partMaterial
import com.willfp.ecoitems.ext.withMaterialArgs
import com.willfp.ecoitems.items.EcoItem
import com.willfp.ecoitems.items.EcoItems
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import wiki.ecohub.core.platform.namespacedKeyOf
import wiki.ecohub.game.item.lookup.LookupArgParser
import java.util.function.Predicate

/**
 * Lets item lookups name materials: `ecoitems:<part> material:<id>` and
 * `ecoitems:<modular item> <slot>:<id> ...`. Lookups hand over the item to modify, so the item is
 * rebuilt with its materials and the rebuilt item's material data is copied across.
 */
object MaterialArgParser : LookupArgParser {
    private val ITEM_KEY = namespacedKeyOf(namespace = "ecoitems", key = "item")

    override fun parseArguments(args: Array<out String>, item: ItemStack): Predicate<ItemStack>? {
        val ecoItem = EcoItems.getByID(item.persistentDataContainer.get(ITEM_KEY, PersistentDataType.STRING))
            ?.takeIf { it.requestsMaterials(args) }
            ?: return null

        return builtWithMaterials(ecoItem, args)?.let { matching(built = it, item = item) } ?: Predicate { false }
    }

    private fun EcoItem.requestsMaterials(args: Array<out String>): Boolean {
        val requested = materialArgs(args.toList()).keys
        return part != null && "material" in requested || modular?.run { slots.keys }.orEmpty().any { it in requested }
    }

    private fun matching(built: ItemStack, item: ItemStack): Predicate<ItemStack> {
        built.itemMeta?.let { from -> item.editMeta { copyMaterialData(from = from, to = it) } }

        val partMaterial = built.partMaterial
        val modularMaterials = built.modularMaterials
        return Predicate { test -> test.partMaterial == partMaterial && test.modularMaterials == modularMaterials }
    }

    override fun serializeBack(item: ItemStack): String? {
        val data = item.persistentDataContainer
        val part = data.get(PART_MATERIAL_KEY, PersistentDataType.STRING)?.let { "material:$it" }
        val modular = data.get(MODULAR_MATERIALS_KEY, PersistentDataType.STRING)
            ?.run { replace(oldValue = "=", newValue = ":").replace(oldValue = ",", newValue = " ") }

        return part ?: modular
    }

    private fun builtWithMaterials(item: EcoItem, args: Array<out String>): ItemStack? =
        item.itemStack.withMaterialArgs(args.toList())
            .takeIf { it.partMaterial != null || it.modularMaterials.isNotEmpty() }

    private fun copyMaterialData(from: ItemMeta, to: ItemMeta) {
        from.persistentDataContainer.copyTo(to.persistentDataContainer, true)

        if (from.hasItemName()) {
            to.itemName(from.itemName())
        }
        to.lore(from.lore())

        if (from.hasCustomModelDataComponent()) {
            to.setCustomModelDataComponent(from.customModelDataComponent)
        }

        if (from is Damageable && to is Damageable && from.hasMaxDamage()) {
            to.setMaxDamage(from.maxDamage)
        }

        if (from.hasAttributeModifiers()) {
            to.attributeModifiers = from.attributeModifiers
        }

        if (from.hasTool()) {
            to.setTool(from.tool)
        }
    }
}
