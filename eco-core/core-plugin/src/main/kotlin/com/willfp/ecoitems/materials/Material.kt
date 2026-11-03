package com.willfp.ecoitems.materials

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.TestableItem
import com.willfp.eco.core.recipe.parts.EmptyTestableItem
import com.willfp.eco.core.registry.Registrable
import com.willfp.ecoitems.plugin
import com.willfp.libreforge.ViolationContext
import com.willfp.libreforge.conditions.ConditionList
import com.willfp.libreforge.conditions.Conditions
import com.willfp.libreforge.effects.EffectList
import com.willfp.libreforge.effects.Effects
import org.bukkit.inventory.ItemStack
import java.util.Objects
import java.util.concurrent.ConcurrentHashMap

/**
 * A material that parts are made from. Modular items store only which material is in each of their
 * slots; their name, stats and look are rebuilt from the materials.
 */
class Material(
    private val materialId: String,
    val config: Config
) : Registrable {
    private val context = ViolationContext(plugin, "Material ID $materialId")

    /** The display name, with colour codes. */
    val name: String = config.getString("name")

    /** Lines shown through %material_lore% in a modular item's lore, whatever slot the material is in. */
    val lore: List<String> = config.getStrings("lore")

    /** The fallback tint colour, also the base for a generated palette. */
    val color: Int = parseColor(config.getStringOrNull("color")) ?: 0xFFFFFF

    /** The palette mapped onto the shared greyscale ramp, darkest to lightest. */
    val palette: List<Int> = config.getStrings("palette")
        .mapNotNull { parseColor(it) }
        .takeIf { it.size == MaterialPalettes.RAMP.size }
        ?: MaterialPalettes.generate(color)

    /** Resolved on first use, so materials can name EcoItems that load after them. */
    val items: List<TestableItem> by lazy {
        config.getStrings("items")
            .map { Items.lookup(it) }
            .filterNot { it is EmptyTestableItem }
    }

    val conditions: ConditionList = Conditions.compile(
        config.getSubsections("conditions"),
        context.with("conditions")
    )

    private val slotEffects = ConcurrentHashMap<String, EffectList>()

    /** The material's settings for one slot of a modular item, or null if it gives nothing there. */
    fun slot(slotId: String): Config? = config.getSubsectionOrNull("slots.$slotId")

    /** Lines shown through %material_lore% only while the material is in [slotId]. */
    fun loreIn(slotId: String): List<String> = slot(slotId)?.getStrings("lore").orEmpty()

    /** Textures drawn on a modular item when this material is in [slotId], never recoloured. */
    fun overlaysIn(slotId: String): List<String> = slot(slotId)?.getStrings("overlays").orEmpty()

    /** Every overlay texture across all slots. */
    val allOverlays: List<String>
        get() = config.getSubsection("slots").getKeys(false).flatMap { overlaysIn(it) }

    /** The traits plus the slot's own effects, compiled once per slot. */
    fun effectsInSlot(slotId: String): EffectList = slotEffects.computeIfAbsent(slotId) {
        Effects.compile(
            config.getSubsections("traits") + slot(slotId)?.getSubsections("effects").orEmpty(),
            context.with("slot $slotId")
        )
    }

    fun matches(item: ItemStack?): Boolean = items.any { it.matches(item) }

    override fun getID(): String = materialId

    override fun equals(other: Any?): Boolean = other is Material && other.materialId == materialId

    override fun hashCode(): Int = Objects.hash(materialId)

    override fun toString(): String = "Material{$materialId}"
}

private const val HEX_RADIX = 16

private fun parseColor(value: String?): Int? = value?.run { removePrefix("#").toIntOrNull(HEX_RADIX) }
