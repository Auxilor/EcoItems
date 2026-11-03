package com.willfp.ecoitems.materials

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.config.toPlainValues
import com.willfp.eco.core.items.legacyToComponentValue
import com.willfp.eco.util.formatEco
import com.willfp.ecoitems.ext.modularMaterials
import com.willfp.ecoitems.ext.partMaterial
import com.willfp.ecoitems.items.EcoItem
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import org.bukkit.inventory.ItemStack

private const val FALLBACK_COLOR = 0xFFFFFF

internal fun ItemStack.materialComponents(item: EcoItem): Map<String, Any?>? {
    val part = item.part
    val modular = item.modular

    return when {
        part != null -> partMaterial?.let { part.components(it) }
        modular != null -> modularMaterials
            .takeIf { it.keys == modular.slots.keys }
            ?.let { modular.components(it, item.config) }
        else -> null
    }
}

private fun PartConfig.components(material: Material): Map<String, Any?> = mapOf(
    "minecraft:item_name" to legacyToComponentValue(
        name.replace(oldValue = "%material%", newValue = material.name).formatEco()
    ),
    "minecraft:custom_model_data" to mapOf("strings" to listOf(material.getID()), "colors" to listOf(material.color))
)

private fun String.withMaterialNames(materials: Map<String, Material>): String =
    materials.entries.fold(this) { text, (slotId, material) ->
        text.replace(oldValue = "%material_$slotId%", newValue = material.name)
    }

private fun Config.plainComponents(): Map<String, Any?> = getSubsection("components").toPlainValues()
    .mapKeys { (key, _) -> if (":" in key) key else "minecraft:$key" }

/** What every slot's material adds up to before it becomes components. */
private class SlotTotals {
    val attributeModifiers = CopyOnWriteArrayList<Any?>()
    val others = ConcurrentHashMap<String, Any>()
    var maxDamage = 0.0
    var durabilityMultiplier = 1.0

    fun add(slotId: String, slotConfig: Config) {
        durabilityMultiplier *= slotConfig.getDoubleOrNull("durability-multiplier") ?: 1.0

        for ((key, value) in slotConfig.plainComponents()) {
            when (key) {
                "minecraft:attribute_modifiers" -> addModifiers(slotId, value as? List<*>)
                "minecraft:max_damage" -> maxDamage += (value as? Number)?.toDouble() ?: 0.0
                else -> value?.let { others.putIfAbsent(key, it) }
            }
        }
    }

    private fun addModifiers(slotId: String, modifiers: List<*>?) {
        modifiers.orEmpty().forEachIndexed { index, modifier ->
            (modifier as? Map<*, *>)?.let { attributeModifiers += it + ("id" to "ecoitems:material_${slotId}_$index") }
        }
    }
}

private fun ModularConfig.components(materials: Map<String, Material>, itemConfig: Config): Map<String, Any?> {
    val totals = SlotTotals()
    totals.attributeModifiers.addAll(
        (itemConfig.getSubsection("item").plainComponents()["minecraft:attribute_modifiers"] as? List<*>).orEmpty()
    )

    for ((slotId, material) in materials) {
        material.slot(slotId)?.let { totals.add(slotId, it) }
    }

    val ownComponents = itemConfig.getSubsection("item").plainComponents().keys
    val result = totals.others.filterKeys { it !in ownComponents }.toMutableMap<String, Any?>()

    if (totals.attributeModifiers.isNotEmpty()) {
        result["minecraft:attribute_modifiers"] = totals.attributeModifiers
    }

    if (totals.maxDamage > 0) {
        result["minecraft:max_damage"] = (totals.maxDamage * totals.durabilityMultiplier).toInt().coerceAtLeast(1)
    }

    miningComponent(materials)?.let { result["minecraft:tool"] = it }

    return result + displayComponents(materials)
}

private fun ModularConfig.displayComponents(materials: Map<String, Material>): Map<String, Any?> = buildMap {
    put(
        key = "minecraft:custom_model_data",
        value = mapOf(
            "strings" to layers.map { materials[it]?.getID().orEmpty() },
            "colors" to layers.map { materials[it]?.color ?: FALLBACK_COLOR }
        )
    )
    put(key = "minecraft:item_name", value = legacyToComponentValue(name.withMaterialNames(materials).formatEco()))

    if (lore.isNotEmpty()) {
        put(
            key = "minecraft:lore",
            value = lore.map { legacyToComponentValue("&r${it.withMaterialNames(materials)}".formatEco()) }
        )
    }
}

private fun ModularConfig.miningComponent(materials: Map<String, Material>): Map<String, Any?>? {
    val blocks = miningBlocks ?: return null
    val slotConfig = miningSlot?.let { materials[it]?.slot(it) }
    val tier = slotConfig?.getStringOrNull("mining-tier")
    val speed = slotConfig?.getDoubleOrNull("mining-speed") ?: 1.0

    return mapOf(
        "default_mining_speed" to 1.0,
        "damage_per_block" to 1,
        "rules" to listOfNotNull(
            tier?.let { mapOf("blocks" to "#minecraft:incorrect_for_${it}_tool", "correct_for_drops" to false) },
            mapOf("blocks" to blocks, "speed" to speed, "correct_for_drops" to true)
        )
    )
}
