package com.willfp.ecoitems.items

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.display.Display
import com.willfp.eco.core.items.CustomItem
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.core.recipe.Recipes
import com.willfp.ecoitems.fuels.Fuels
import com.willfp.libreforge.Holder
import com.willfp.libreforge.conditions.Conditions
import com.willfp.libreforge.effects.Effects
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.Objects

class EcoItem(
    override val id: String,
    val config: Config,
    private val plugin: EcoPlugin
) : Holder {
    override val effects = config.getSubsections("effects").mapNotNull {
        Effects.compile(it, "Item ID $id")
    }.toSet()

    override val conditions = config.getSubsections("conditions").mapNotNull {
        Conditions.compile(it, "Item ID $id")
    }.toSet()

    val lore: List<String> = config.getStrings("item.lore")

    val displayName: String = config.getString("item.displayName")

    private val _itemStack: ItemStack = run {
        val itemConfig = config.getSubsection("item")
        ItemStackBuilder(Items.lookup(itemConfig.getString("item")).item).apply {
            setDisplayName(itemConfig.getFormattedString("displayName"))
            addLoreLines(
                itemConfig.getFormattedStrings("lore").map { "${Display.PREFIX}$it" })
            writeMetaKey(
                plugin.namespacedKeyFactory.create("item"),
                PersistentDataType.STRING,
                id
            )
        }.build()
    }

    val itemStack: ItemStack
        get() = _itemStack.clone()

    val effectiveDurability = config.getIntOrNull("item.effectiveDurability") ?: itemStack.type.maxDurability.toInt()

    val customItem = CustomItem(
        plugin.namespacedKeyFactory.create(id),
        { test -> ItemUtils.getEcoItem(test) == this },
        itemStack
    ).apply { register() }

    val craftingRecipe = if (config.getBool("item.craftable")) {
        Recipes.createAndRegisterRecipe(
            plugin,
            id,
            itemStack,
            config.getStrings("item.recipe"),
            config.getStringOrNull("item.craftingPermission")
        )
    } else null

    val fuels = config.getStrings("fuels").mapNotNull { Fuels.getByID(it) }

    val baseDamage = config.getDouble("baseDamage")

    val baseAttackSpeed = config.getDouble("baseAttackSpeed")

    override fun equals(other: Any?): Boolean {
        if (other !is EcoItem) {
            return false
        }

        return this.id == other.id
    }

    override fun hashCode(): Int {
        return Objects.hash(this.id)
    }

    override fun toString(): String {
        return "EcoItem{$id}"
    }
}