package com.willfp.ecoitems.items

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.display.Display
import com.willfp.eco.core.items.CustomItem
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.core.recipe.Recipes
import com.willfp.eco.core.recipe.recipes.CraftingRecipe
import com.willfp.eco.core.registry.Registrable
import com.willfp.ecoitems.BuildConfig
import com.willfp.ecoitems.blocks.EcoBlock
import com.willfp.ecoitems.furniture.Furniture
import com.willfp.ecoitems.nms.ItemComponentsProxy
import com.willfp.ecoitems.nms.toComponentValues
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

    val lore: List<String> = config.getStrings("item.lore")

    val displayName: String? = config.getStringOrNull("item.display-name")

    val slot = SlotTypes[config.getString("slot")] ?: SlotTypeMainhand

    // Defensive copy
    private val _itemStack: ItemStack = run {
        val itemConfig = config.getSubsection("item")
        val built = ItemStackBuilder(Items.lookup(itemConfig.getString("item")).item).apply {
            if (itemConfig.has("display-name")) {
                setDisplayName(itemConfig.getFormattedString("display-name"))
            }
            addLoreLines(
                itemConfig.getFormattedStrings("lore").map { "${Display.PREFIX}$it" }
            )
        }.build().withComponents(itemConfig)

        built.apply {
            ecoItem = this@EcoItem
        }
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
        if (!config.getBool("item.craftable")) return

        val recipeStrings = config.getStrings("item.recipe")
        if (recipeStrings.isEmpty()) return

        craftingRecipe = Recipes.createAndRegisterRecipe(
            plugin,
            id.key,
            itemStack.apply {
                amount = config.getIntOrNull("item.recipe-give-amount") ?: 1
            },
            recipeStrings,
            config.getStringOrNull("item.crafting-permission"),
            config.getBool("item.shapeless")
        )
    }

    val rarity = Rarities[config.getString("rarity")]

    private fun ItemStack.withComponents(itemConfig: Config): ItemStack {
        // Keys without a namespace are minecraft components - saves quoting.
        val components = itemConfig.getSubsection("components").toComponentValues()
            .mapKeys { (key, _) -> if (":" in key) key else "minecraft:$key" }
            .toMutableMap()

        // name is shorthand for the item_name component (unlike display-name,
        // which sets a custom name through the display system).
        if (itemConfig.has("name")) {
            components.putIfAbsent("minecraft:item_name", itemConfig.getFormattedString("name"))
        }

        val blockAssets = this@EcoItem.block?.hasAssets == true
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
