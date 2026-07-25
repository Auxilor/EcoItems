package com.willfp.ecoitems.items

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.TestableItem
import com.willfp.eco.core.recipe.parts.EmptyTestableItem
import com.willfp.eco.core.recipe.workstation.AnvilRecipe
import com.willfp.eco.core.recipe.workstation.BrewingRecipe
import com.willfp.eco.core.recipe.workstation.SmeltingRecipe
import com.willfp.eco.core.recipe.workstation.SmeltingType
import com.willfp.eco.core.recipe.workstation.SmithingRecipe
import com.willfp.eco.core.recipe.workstation.StonecuttingRecipe
import com.willfp.eco.core.recipe.workstation.WorkstationRecipe
import com.willfp.ecoitems.plugin
import org.bukkit.inventory.ItemStack

/**
 * Non-crafting-table recipes, registered against eco's shared workstation recipe
 * registry - eco itself owns the listeners that match them, so registering here is
 * all that's needed for the recipe to work in-game.
 *
 * Ingredient key names deliberately mirror EcoCrafting's so a recipe reads the same
 * in either plugin. Villager trading and grindstones stay EcoCrafting-exclusive, as
 * do the extras EcoItems doesn't carry (conditions, effects, prices, locking, and
 * the recipe book) - those keys are simply ignored if present.
 */

/** An ingredient: the predicate used to match it, and the item shown for it in vanilla UIs. */
private data class Ingredient(
    val testable: TestableItem,
    val display: ItemStack
)

private val SMELTING_TYPES = mapOf(
    "furnace" to SmeltingType.FURNACE,
    "blast_furnace" to SmeltingType.BLAST_FURNACE,
    "smoker" to SmeltingType.SMOKER,
    "campfire" to SmeltingType.CAMPFIRE
)

/** Every workstation an item's recipe type may name. */
internal val WORKSTATIONS = SMELTING_TYPES.keys +
        setOf("crafting_table", "stonecutter", "smithing_table", "anvil", "brewing_stand")

/**
 * Register this item's recipe at [workstation], reading its ingredients from the item's
 * recipes section. Does nothing if an ingredient doesn't resolve - a half-registered
 * recipe would match on the wrong items, so a bad lookup drops the whole recipe.
 */
internal fun EcoItem.registerWorkstationRecipe(workstation: String, recipeConfig: Config) {
    val output = itemStack.apply {
        amount = recipeConfig.getIntOrNull("give-amount") ?: 1
    }

    val permission = recipeConfig.getStringOrNull("permission")?.takeIf { it.isNotBlank() }
    val smeltingType = SMELTING_TYPES[workstation]

    val recipe: WorkstationRecipe = when {
        smeltingType != null -> {
            val input = ingredient(recipeConfig, "input") ?: return

            SmeltingRecipe.builder(id, output, input.testable, smeltingType)
                .inputDisplay(input.display)
                // -1 keeps the per-workstation vanilla default.
                .cookTime(recipeConfig.getIntOrNull("cook-time") ?: -1)
                .experience(recipeConfig.getStringOrNull("experience")?.toFloatOrNull() ?: 0f)
                .also { builder -> permission?.let { builder.permission(it) } }
                .build()
        }

        workstation == "stonecutter" -> {
            val input = ingredient(recipeConfig, "input") ?: return

            StonecuttingRecipe.builder(id, output, input.testable)
                .inputDisplay(input.display)
                .also { builder -> permission?.let { builder.permission(it) } }
                .build()
        }

        workstation == "smithing_table" -> {
            val template = ingredient(recipeConfig, "template") ?: return
            val base = ingredient(recipeConfig, "base") ?: return
            val addition = ingredient(recipeConfig, "addition") ?: return

            SmithingRecipe.builder(id, output)
                .template(template.testable, template.display)
                .base(base.testable, base.display)
                .addition(addition.testable, addition.display)
                .also { builder -> permission?.let { builder.permission(it) } }
                .build()
        }

        workstation == "anvil" -> {
            val base = ingredient(recipeConfig, "base") ?: return
            // The second anvil slot is optional - no material means the base alone matches.
            val material = if (recipeConfig.getStringOrNull("material")?.isNotBlank() == true) {
                ingredient(recipeConfig, "material") ?: return
            } else {
                null
            }

            // No result-name: the result is the item, which carries its own name.
            AnvilRecipe.builder(id, output, base.testable)
                .baseDisplay(base.display)
                .material(material?.testable)
                .materialDisplay(material?.display)
                .repairCost(recipeConfig.getIntOrNull("repair-cost") ?: 1)
                .also { builder -> permission?.let { builder.permission(it) } }
                .build()
        }

        workstation == "brewing_stand" -> {
            val base = ingredient(recipeConfig, "base") ?: return
            val brewingIngredient = ingredient(recipeConfig, "ingredient") ?: return

            BrewingRecipe.builder(id, output, base.testable, brewingIngredient.testable)
                .brewTime(recipeConfig.getIntOrNull("brew-time") ?: 400)
                .also { builder -> permission?.let { builder.permission(it) } }
                .build()
        }

        else -> {
            plugin.logger.warning(
                "Item ${id.key} has an unknown recipe type '$workstation', so its recipe was skipped " +
                        "(valid: ${WORKSTATIONS.sorted().joinToString(", ")})"
            )
            return
        }
    }

    recipe.register()
    WorkstationRecipePermissions.track(recipe)
}

/** Look up the ingredient at [key], warning and returning null if it doesn't resolve. */
private fun EcoItem.ingredient(recipeConfig: Config, key: String): Ingredient? {
    val lookup = recipeConfig.getStringOrNull(key)

    if (lookup.isNullOrBlank()) {
        plugin.logger.warning("Item ${id.key} is missing '$key' for its recipe, so the recipe was skipped")
        return null
    }

    val testable = Items.lookup(lookup)

    if (testable is EmptyTestableItem) {
        plugin.logger.warning("Item ${id.key} has an invalid recipe $key '$lookup', so the recipe was skipped")
        return null
    }

    return Ingredient(testable, testable.item.clone())
}
