package com.willfp.ecoitems.migration

import org.bukkit.configuration.ConfigurationSection

/**
 * ItemsAdder recipes, which live in a `recipes:` section alongside the items
 * rather than on the item itself. They're collected up front, keyed by the item
 * each one produces, so an item can pick its recipe up whichever file it came from.
 *
 * IA allows several recipes per item; we take the first and skip the rest, since
 * an EcoItem has one recipe. Anvil repairs are skipped entirely - they restore
 * durability rather than produce an item, so they have no equivalent here.
 */
internal class ItemsAdderRecipes(private val result: MigrationResult) {
    private val byItem = mutableMapOf<String, Map<String, Any>>()

    // Rotations and mirrors of one recipe are usually written out as extra patterns
    // or extra recipes, so these are the norm rather than mistakes - counted and
    // reported once at the end instead of once each.
    private var duplicates = 0
    private var extraPatterns = 0

    operator fun get(itemId: String): Map<String, Any>? = byItem[itemId]

    /** Report what was collapsed, once the whole setup has been read. */
    fun finish() {
        if (duplicates > 0) {
            result.warn(
                "Recipes: kept one recipe per item and skipped $duplicates others " +
                        "(rotated and mirrored variants don't need their own recipe here)"
            )
        }
        if (extraPatterns > 0) {
            result.warn("Recipes: used the first pattern of $extraPatterns recipes that had several")
        }
    }

    fun load(section: ConfigurationSection) {
        for (type in section.getKeys(false)) {
            val group = section.getConfigurationSection(type) ?: continue

            for (name in group.getKeys(false)) {
                val recipe = group.getConfigurationSection(name) ?: continue
                if (recipe.contains("enabled") && !recipe.getBoolean("enabled")) continue

                if (type == "anvil_repair") {
                    result.warn("Recipe $name: anvil repairs have no EcoItems equivalent and were skipped")
                    continue
                }

                val built = when (type) {
                    "crafting_table" -> crafting(recipe)
                    "cooking" -> cooking(name, recipe)
                    "campfire_cooking" -> campfire(recipe)
                    "smithing" -> smithing(recipe)
                    "brewing" -> brewing(recipe)
                    "stonecutter" -> stonecutter(recipe)
                    else -> {
                        result.warn("Recipe $name: recipe type '$type' is not supported and was skipped")
                        null
                    }
                } ?: continue

                val itemId = itemId(recipe.getString("result.item")) ?: run {
                    result.warn("Recipe $name: only recipes resulting in a converted item are supported")
                    null
                } ?: continue

                val extras = buildMap {
                    recipe.getString("permission")?.takeIf { it.isNotBlank() }?.let { put("permission", it) }
                    recipe.getInt("result.amount", 1).takeIf { it > 1 }?.let { put("give-amount", it) }
                }

                if (byItem.putIfAbsent(itemId, built + extras) != null) {
                    duplicates++
                }
            }
        }
    }

    private fun crafting(recipe: ConfigurationSection): Map<String, Any>? {
        val ingredients = recipe.getConfigurationSection("ingredients") ?: return null

        if (recipe.getBoolean("shapeless")) {
            val parts = ingredients.getKeys(false).mapNotNull { lookup(ingredients.getString(it)) }
            if (parts.isEmpty()) return null
            return mapOf("shapeless" to true, "recipe" to parts)
        }

        // IA writes each rotation as pattern_2, pattern_3, and so on.
        if (recipe.getKeys(false).any { it.startsWith("pattern_") }) {
            extraPatterns++
        }

        // IA patterns can be smaller than 3x3 (a 2x2 recipe is two rows of two),
        // so pad them out to the 9 slots our recipes always use.
        val rows = recipe.getStringList("pattern").take(3).map { it.padEnd(3).take(3) }
        if (rows.isEmpty()) return null
        val grid = rows + List(3 - rows.size) { "   " }

        val slots = grid.flatMap { row ->
            row.map { char ->
                val key = char.toString()
                // X is IA's empty slot, but only if it isn't a declared ingredient.
                if (ingredients.contains(key)) lookup(ingredients.getString(key)) ?: "air" else "air"
            }
        }

        return if (slots.all { it == "air" }) null else mapOf("recipe" to slots)
    }

    private fun cooking(name: String, recipe: ConfigurationSection): Map<String, Any>? {
        val input = lookup(recipe.getString("ingredient.item")) ?: return null
        val machines = recipe.getStringList("machines")

        if (machines.size > 1) {
            result.warn("Recipe $name: imported as ${machines.first().lowercase()} only, an item can have one recipe")
        }

        val type = when (machines.firstOrNull()?.uppercase()) {
            "BLAST_FURNACE" -> "blast_furnace"
            "SMOKER" -> "smoker"
            "CAMPFIRE" -> "campfire"
            else -> "furnace"
        }

        return cooked(type, input, recipe)
    }

    private fun campfire(recipe: ConfigurationSection): Map<String, Any>? {
        val input = lookup(recipe.getString("ingredient.item")) ?: return null
        return cooked("campfire", input, recipe)
    }

    private fun cooked(type: String, input: String, recipe: ConfigurationSection) = buildMap<String, Any> {
        put("type", type)
        put("input", input)
        if (recipe.contains("exp")) put("experience", recipe.getDouble("exp"))
        if (recipe.contains("cook_time")) put("cook-time", recipe.getInt("cook_time"))
    }

    private fun smithing(recipe: ConfigurationSection): Map<String, Any>? {
        val base = lookup(recipe.getString("base")) ?: return null
        val addition = lookup(recipe.getString("addition")) ?: return null
        val template = lookup(recipe.getString("template")) ?: return null

        return mapOf(
            "type" to "smithing_table",
            "template" to template,
            "base" to base,
            "addition" to addition
        )
    }

    private fun brewing(recipe: ConfigurationSection): Map<String, Any>? {
        val base = lookup(recipe.getString("base.item")) ?: return null
        val ingredient = lookup(recipe.getString("ingredient.item")) ?: return null

        return buildMap {
            put("type", "brewing_stand")
            put("base", base)
            put("ingredient", ingredient)
            if (recipe.contains("brew_time")) put("brew-time", recipe.getInt("brew_time"))
        }
    }

    private fun stonecutter(recipe: ConfigurationSection): Map<String, Any>? {
        val input = lookup(recipe.getString("ingredient.item")) ?: return null
        return mapOf("type" to "stonecutter", "input" to input)
    }

    /** The item an ingredient refers to, as one of our lookup strings. */
    private fun lookup(raw: String?): String? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null

        if (":" !in value) {
            return value.lowercase()
        }

        val namespace = value.substringBefore(":")
        val name = value.substringAfterLast(":")

        return if (namespace.equals("minecraft", ignoreCase = true)) name.lowercase() else "ecoitems:$name"
    }

    /** IA results are namespaced; our ids are the bare item name. */
    private fun itemId(raw: String?): String? =
        raw?.trim()?.takeIf { it.isNotEmpty() }?.substringAfterLast(":")
}
