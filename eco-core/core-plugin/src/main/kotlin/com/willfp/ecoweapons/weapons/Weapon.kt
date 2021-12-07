package com.willfp.ecoweapons.weapons

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.display.Display
import com.willfp.eco.core.items.CustomItem
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.core.recipe.Recipes
import com.willfp.ecoweapons.fuels.Fuels
import com.willfp.libreforge.Holder
import com.willfp.libreforge.conditions.Conditions
import com.willfp.libreforge.effects.Effects
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.Objects
import java.util.UUID

class Weapon(
    private val config: Config,
    private val plugin: EcoPlugin
) : Holder {
    val id = config.getString("id")

    override val effects = config.getSubsections("effects").mapNotNull {
        Effects.compile(it, "Weapon ID $id")
    }.toSet()

    override val conditions = config.getSubsections("conditions").mapNotNull {
        Conditions.compile(it, "Weapon ID $id")
    }.toSet()

    val itemStack: ItemStack = run {
        val itemConfig = config.getSubsection("item")
        val item = ItemStackBuilder(Items.lookup(itemConfig.getString("item")).item).apply {
            setDisplayName(itemConfig.getFormattedString("displayName"))
            addLoreLines(
                itemConfig.getFormattedStrings("lore").map { "${Display.PREFIX}$it" })
            writeMetaKey(
                this@Weapon.plugin.namespacedKeyFactory.create("weapon"),
                PersistentDataType.STRING,
                this@Weapon.id
            )
        }.build()
        val meta = item.itemMeta!!
        meta.addAttributeModifier(
            Attribute.GENERIC_ATTACK_DAMAGE,
            AttributeModifier(
                UUID.nameUUIDFromBytes("${this.id}_ad".toByteArray()),
                "${this.id}_ad",
                itemConfig.getDouble("attackDamage"),
                AttributeModifier.Operation.ADD_NUMBER
            )
        )
        meta.addAttributeModifier(
            Attribute.GENERIC_ATTACK_SPEED,
            AttributeModifier(
                UUID.nameUUIDFromBytes("${this.id}_as".toByteArray()),
                "${this.id}_as",
                itemConfig.getDouble("attackDamage"),
                AttributeModifier.Operation.ADD_NUMBER
            )
        )
        item.itemMeta = meta

        item
    }

    val customItem = CustomItem(
        plugin.namespacedKeyFactory.create(id),
        { test -> WeaponUtils.getWeaponFromItem(test) == this },
        itemStack
    ).apply { register() }

    val craftingRecipe = if (config.getBool("item.craftable")) {
        Recipes.createAndRegisterRecipe(
            plugin,
            id,
            itemStack,
            config.getStrings("item.recipe")
        )
    } else null

    val fuels = config.getStrings("fuels").mapNotNull { Fuels.getByID(it) }

    override fun equals(other: Any?): Boolean {
        if (other !is Weapon) {
            return false
        }

        return this.id == other.id
    }

    override fun hashCode(): Int {
        return Objects.hash(this.id)
    }

    override fun toString(): String {
        return "Weapon{$id}"
    }
}