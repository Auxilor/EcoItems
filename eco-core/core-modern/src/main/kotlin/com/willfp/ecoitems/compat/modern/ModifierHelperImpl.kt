package com.willfp.ecoitems.compat.modern

import com.willfp.eco.core.EcoPlugin
import com.willfp.ecoitems.items.ModifierHelper
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlotGroup

class ModifierHelperImpl : ModifierHelper {
    override fun createModifier(
        plugin: EcoPlugin,
        attributeType: String,
        amount: Double,
        offset: Int
    ): AttributeModifier {
        @Suppress("UnstableApiUsage")
        return AttributeModifier(
            plugin.createNamespacedKey("${attributeType.lowercase()}_$offset"),
            amount,
            AttributeModifier.Operation.ADD_NUMBER,
            EquipmentSlotGroup.ANY
        )
    }

    override fun isFromEcoItems(modifier: AttributeModifier): Boolean {
        return modifier.key.namespace == "ecoitems" && (modifier.key.key.startsWith("damage")
                || modifier.key.key.startsWith("speed"))
    }
}
