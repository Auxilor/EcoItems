package com.willfp.ecoitems.items

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.display.Display
import com.willfp.eco.core.fast.fast
import com.willfp.eco.core.items.CustomItem
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.core.recipe.Recipes
import com.willfp.eco.core.recipe.parts.EmptyTestableItem
import com.willfp.eco.core.recipe.recipes.CraftingRecipe
import com.willfp.eco.core.registry.Registrable
import com.willfp.ecoitems.BuildConfig
import com.willfp.ecoitems.blocks.EcoBlock
import com.willfp.ecoitems.crops.EcoCrop
import com.willfp.ecoitems.furniture.Furniture
import com.willfp.ecoitems.nms.ItemComponentsProxy
import com.willfp.ecoitems.nms.legacyToComponentValue
import com.willfp.ecoitems.nms.toPlainValues
import com.willfp.ecoitems.paintings.Paintings
import com.willfp.ecoitems.plugin
import com.willfp.ecoitems.rarity.Rarities
import com.willfp.ecoitems.sounds.Sounds
import com.willfp.libreforge.Holder
import com.willfp.libreforge.ViolationContext
import com.willfp.libreforge.conditions.Conditions
import com.willfp.libreforge.effects.Effects
import com.willfp.libreforge.slot.SlotTypes
import com.willfp.libreforge.slot.impl.SlotTypeMainhand
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ArmorMeta
import org.bukkit.persistence.PersistentDataType
import java.util.Objects

class EcoItem(
    id: String,
    val config: Config
) : Holder, Registrable {
    override val effects = Effects.compile(
        config.getSubsections("effects"),
        ViolationContext(plugin, "Item ID $id")
    )

    override val conditions = Conditions.compile(
        config.getSubsections("conditions"),
        ViolationContext(plugin, "Item ID $id")
    )

    override val id = plugin.createNamespacedKey(id)

    /** The custom block this item places, if it has a block: section. */
    val block = if (config.has("block")) EcoBlock(id, config.getSubsection("block")) else null

    /** The furniture this item places, if it has a furniture: section. */
    val furniture = if (config.has("furniture")) Furniture(id, config.getSubsection("furniture")) else null

    /** The crop this item plants, if it has a crop: section (the item is the seed). */
    val crop = if (config.has("crop")) {
        if (block != null) {
            plugin.logger.warning("Item $id has both block: and crop: sections; ignoring crop")
            null
        } else {
            EcoCrop(id, config.getSubsection("crop"))
        }
    } else {
        null
    }

    val lore: List<String> = config.getStrings("item.lore")

    val displayName: String? = config.getStringOrNull("item.display-name")

    val slot = SlotTypes[config.getString("slot")] ?: SlotTypeMainhand

    /**
     * Burn time override when used as furnace fuel. The base item must be a
     * vanilla fuel (furnaces only consume those); 0 blocks burning entirely.
     */
    val fuelBurnTicks = config.getIntOrNull("fuel.burn-ticks")

    /** Hidden from the /ecoitems gui browser. */
    val excludeFromGui = config.getBool("exclude-from-gui")

    /** Hidden from command tab-completion (the exact id still works). */
    val excludeFromCommands = config.getBool("exclude-from-commands")

    // Defensive copy
    private val _itemStack: ItemStack = run {
        val itemConfig = config.getSubsection("item")
        val base = Items.lookup(itemConfig.getString("item"))
        if (base is EmptyTestableItem) {
            plugin.logger.warning("Item $id has an invalid base item '${itemConfig.getString("item")}'")
        }
        val displayName = if (itemConfig.has("display-name")) {
            itemConfig.getFormattedString("display-name")
        } else {
            null
        }

        val lore = itemConfig.getFormattedStrings("lore").map { "${Display.PREFIX}$it" }

        val built = ItemStackBuilder(base.item).apply {
            if (displayName != null) {
                setDisplayName(displayName)
            }
            addLoreLines(lore)
        }.build().withComponents(itemConfig)

        built.apply {
            ecoItem = this@EcoItem
        }

        val loreComponents = built.fast().loreComponents.encoded()

        built.itemMeta = built.itemMeta?.apply {
            if (displayName != null) {
                persistentDataContainer.set(baseDisplayNameKey, PersistentDataType.STRING, displayName)
            }
            persistentDataContainer.set(baseLoreKey, PersistentDataType.STRING, lore.joinToString(LORE_SEPARATOR))
            persistentDataContainer.set(baseLoreComponentsKey, PersistentDataType.STRING, loreComponents)
            if (this is ArmorMeta) {
                trim?.let { persistentDataContainer.set(baseTrimKey, PersistentDataType.STRING, it.encoded()) }
            }
        }

        built
    }

    val itemStack: ItemStack
        get() = _itemStack.clone()

    val customItem = CustomItem(
        plugin.namespacedKeyFactory.create(id),
        { test -> test.ecoItem == this },
        itemStack
    ).apply { register() }

    var craftingRecipe: CraftingRecipe? = null
        private set

    fun registerRecipe() {
        // Having a recipe is what makes an item craftable. craftable is kept only
        // so existing configs that switch it off still switch it off.
        if (config.has("item.craftable") && !config.getBool("item.craftable")) return

        // The recipe lives in its own section, keyed the same way as EcoCrafting so
        // a recipe reads the same in either plugin. It's called recipes rather than
        // recipe because item.recipe is already the crafting grid below.
        val recipeConfig = config.getSubsectionOrNull("item.recipes")

        if (recipeConfig != null) {
            val type = recipeConfig.getStringOrNull("type")?.lowercase() ?: "crafting_table"

            if (type == "crafting_table") {
                registerCraftingRecipe(
                    recipeConfig.getStrings("recipe"),
                    recipeConfig.getStringOrNull("permission"),
                    recipeConfig.getBool("shapeless"),
                    recipeConfig.getIntOrNull("give-amount") ?: 1
                )
            } else {
                registerWorkstationRecipe(type, recipeConfig)
            }

            return
        }

        // The original flat layout, kept working for configs written before the
        // recipes section existed. Crafting tables only.
        registerCraftingRecipe(
            config.getStrings("item.recipe"),
            config.getStringOrNull("item.crafting-permission"),
            config.getBool("item.shapeless"),
            config.getIntOrNull("item.recipe-give-amount") ?: 1
        )
    }

    private fun registerCraftingRecipe(
        recipeStrings: List<String>,
        permission: String?,
        shapeless: Boolean,
        giveAmount: Int
    ) {
        if (recipeStrings.isEmpty()) return

        craftingRecipe = Recipes.createAndRegisterRecipe(
            plugin,
            id.key,
            itemStack.apply { amount = giveAmount },
            recipeStrings,
            permission,
            shapeless
        )
    }

    val rarity = Rarities[config.getString("rarity")]

    private fun ItemStack.withComponents(itemConfig: Config): ItemStack {
        // Keys without a namespace are minecraft components - saves quoting.
        val components = itemConfig.getSubsection("components").toPlainValues()
            .mapKeys { (key, _) -> if (":" in key) key else "minecraft:$key" }
            .toMutableMap()

        // name is shorthand for the item_name component (unlike display-name,
        // which sets a custom name through the display system). It's formatted
        // here, so it goes in as a component rather than as a plain string.
        if (itemConfig.has("name")) {
            components.putIfAbsent(
                "minecraft:item_name",
                legacyToComponentValue(itemConfig.getFormattedString("name"))
            )
        }

        // Before components, combat stats were three top-level options holding
        // the item's total value, applied to the player as attribute modifiers
        // while the item was held. They're deprecated, but plenty of configs
        // still have them, so they're converted rather than ignored.
        val legacy = legacyAttributeModifiers(components["minecraft:attribute_modifiers"])
        if (legacy.isNotEmpty()) {
            plugin.logger.warning(
                "Item ${this@EcoItem.id.key} uses the deprecated ${legacy.joinToString { it.option }} " +
                        "option(s); use the minecraft:attribute_modifiers component instead"
            )

            val configured = components["minecraft:attribute_modifiers"] as? List<*> ?: emptyList<Any?>()
            components["minecraft:attribute_modifiers"] = legacy.map { it.modifier } + configured
        }

        val blockAssets = this@EcoItem.block?.hasAssets == true || this@EcoItem.crop?.block?.hasAssets == true
        if (itemConfig.has("texture") || itemConfig.has("model") || itemConfig.has("definition") || blockAssets) {
            if (BuildConfig.FREE_VERSION) {
                plugin.logger.warning(
                    "Item ${this@EcoItem.id.key} has a texture, but item textures require the paid version of EcoItems"
                )
            } else {
                // The pack system generates the matching assets on reload.
                components.putIfAbsent("minecraft:item_model", "ecoitems:${this@EcoItem.id.key}")
            }
        }

        if (components.isEmpty()) {
            return this
        }

        val result = plugin.getProxy(ItemComponentsProxy::class.java)
            .withComponents(this, components)

        for (error in result.errors) {
            val pending = pendingRegistration(error)
            if (pending != null) {
                plugin.logger.warning(
                    "Item ${this@EcoItem.id.key} references the $pending, which registers on the next server restart"
                )
            } else {
                plugin.logger.warning("Invalid component on item ${this@EcoItem.id.key}: $error")
            }
        }

        return result.item
    }

    /**
     * The [LegacyAttribute] options this item sets, as attribute modifier
     * entries. Legacy values are totals, so the modifier is the total minus the
     * player's base value, and it takes over the vanilla modifier's id so it
     * replaces the base item's own value rather than stacking on top of it.
     *
     * An option is skipped when the component already configures that
     * attribute, so a half-migrated config doesn't count its damage twice.
     */
    private fun legacyAttributeModifiers(configured: Any?): List<LegacyModifier> {
        val configuredTypes = (configured as? List<*>).orEmpty()
            .filterIsInstance<Map<*, *>>()
            .mapNotNull { it["type"]?.toString() }
            .map { if (":" in it) it else "minecraft:$it" }

        return LegacyAttribute.entries.mapNotNull { attribute ->
            if (attribute.type in configuredTypes) {
                return@mapNotNull null
            }

            val total = config.getDoubleOrNull(attribute.option) ?: return@mapNotNull null

            LegacyModifier(
                attribute.option,
                mapOf(
                    "type" to attribute.type,
                    "id" to attribute.modifierId,
                    "amount" to total - attribute.playerBase,
                    "operation" to "add_value",
                    "slot" to "mainhand"
                )
            )
        }
    }

    /**
     * Paintings and jukebox songs register through a generated datapack, which
     * only loads at server start - so a registry miss on one of our own entries
     * means a pending restart, not a config mistake.
     */
    private fun pendingRegistration(error: String): String? {
        if (BuildConfig.FREE_VERSION || "Failed to get element" !in error) {
            return null
        }

        val referenced = "ecoitems:([a-z0-9_]+)".toRegex().find(error)?.groupValues?.get(1) ?: return null

        return when {
            "painting/variant" in error && Paintings[referenced] != null ->
                "painting '$referenced'"
            "jukebox_playable" in error && Sounds[referenced]?.jukebox != null ->
                "jukebox song '$referenced'"
            else -> null
        }
    }

    override fun getID(): String {
        return this.id.key
    }

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

/**
 * A pre-components combat stat option, and the vanilla attribute modifier it
 * converts into. The base values are the player's own, which every attribute
 * modifier is applied on top of: https://minecraft.wiki/w/Attribute
 */
private enum class LegacyAttribute(
    val option: String,
    val type: String,
    val modifierId: String,
    val playerBase: Double
) {
    BASE_DAMAGE("base-damage", "minecraft:attack_damage", "minecraft:base_attack_damage", 1.0),
    BASE_ATTACK_SPEED("base-attack-speed", "minecraft:attack_speed", "minecraft:base_attack_speed", 4.0),
    BASE_ATTACK_RANGE(
        "base-attack-range",
        "minecraft:entity_interaction_range",
        "minecraft:base_entity_interaction_range",
        3.0
    )
}

private data class LegacyModifier(
    val option: String,
    val modifier: Map<String, Any>
)
