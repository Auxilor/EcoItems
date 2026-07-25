package com.willfp.ecoitems.pack

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.willfp.ecoitems.nms.toPlainValues

/**
 * Composes the item definition node tree (the "model" value of
 * assets/<ns>/items/<id>.json).
 *
 * A raw `definition` config is passed through verbatim, exactly like
 * item.components - the vanilla format covers every present and future node
 * type. Otherwise the convenience states wrap the base model leaf in the
 * same tree shapes the vanilla items use.
 */
object ItemDefinitionBuilder {
    private val gson = Gson()

    class Definitions(
        val main: JsonObject,
        val throwing: JsonObject?
    )

    fun build(asset: ItemPackAsset, baseModelKey: String): Definitions {
        // The raw escape hatch wins over everything.
        asset.definition?.let { definition ->
            return Definitions(gson.toJsonTree(definition.toPlainValues()).asJsonObject, throwingNode(asset))
        }

        var node = leaf(baseModelKey, asset.dyeTint)

        // Innermost to outermost, mirroring the vanilla trees.
        asset.states["damaged"]?.let { damaged ->
            node = rangeDispatch(
                property = "minecraft:damage",
                entries = damaged.locations.mapIndexed { index, location ->
                    entry((index + 1.0) / (damaged.locations.size + 1), leaf(location.key))
                },
                fallback = node
            )
        }

        asset.states["broken"]?.let { node = conditionSwap("minecraft:broken", node, it) }
        asset.states["cast"]?.let { node = conditionSwap("minecraft:fishing_rod/cast", node, it) }
        asset.states["blocking"]?.let { node = conditionSwap("minecraft:using_item", node, it) }

        asset.states["pulling"]?.let { pulling ->
            val stages = pulling.locations.map { leaf(it.key) }

            val dispatch = rangeDispatch(
                property = if (asset.isCrossbow) "minecraft:crossbow/pull" else "minecraft:use_duration",
                // The first stage is the fallback; the rest at vanilla-style
                // thresholds from 0.65 to 0.9.
                entries = stages.drop(1).mapIndexed { index, stage ->
                    val count = stages.size - 1
                    entry(0.65 + (0.9 - 0.65) * index / (count - 1).coerceAtLeast(1), stage)
                },
                fallback = stages.first()
            )
            if (!asset.isCrossbow) {
                dispatch.addProperty("scale", 0.05)
            }

            node = condition("minecraft:using_item", onFalse = node, onTrue = dispatch)
        }

        val charged = asset.states["charged"]
        val firework = asset.states["firework"]
        if (charged != null || firework != null) {
            val cases = JsonArray()
            charged?.let { cases.add(case("arrow", leaf(it.locations.first().key))) }
            firework?.let { cases.add(case("rocket", leaf(it.locations.first().key))) }

            node = JsonObject().apply {
                addProperty("type", "minecraft:select")
                addProperty("property", "minecraft:charge_type")
                add("cases", cases)
                add("fallback", node)
            }
        }

        return Definitions(node, throwingNode(asset))
    }

    private fun throwingNode(asset: ItemPackAsset): JsonObject? =
        asset.throwing?.let { leaf(it.locations.first().key) }

    private fun leaf(modelKey: String, dyeTint: Int? = null): JsonObject = JsonObject().apply {
        addProperty("type", "minecraft:model")
        addProperty("model", modelKey)
        // Re-declare the dye tint dropped by overriding item_model, so
        // tintindex faces pick up the item's dyed_color (default when unset).
        if (dyeTint != null) {
            add("tints", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("type", "minecraft:dye")
                    addProperty("default", dyeTint)
                })
            })
        }
    }

    private fun condition(property: String, onFalse: JsonObject, onTrue: JsonObject): JsonObject =
        JsonObject().apply {
            addProperty("type", "minecraft:condition")
            addProperty("property", property)
            add("on_false", onFalse)
            add("on_true", onTrue)
        }

    private fun conditionSwap(property: String, base: JsonObject, state: StateModel): JsonObject =
        condition(property, onFalse = base, onTrue = leaf(state.locations.first().key))

    private fun rangeDispatch(property: String, entries: List<JsonObject>, fallback: JsonObject): JsonObject =
        JsonObject().apply {
            addProperty("type", "minecraft:range_dispatch")
            addProperty("property", property)
            add("entries", JsonArray().apply { entries.forEach { add(it) } })
            add("fallback", fallback)
        }

    private fun entry(threshold: Double, model: JsonObject): JsonObject = JsonObject().apply {
        addProperty("threshold", threshold)
        add("model", model)
    }

    private fun case(`when`: String, model: JsonObject): JsonObject = JsonObject().apply {
        addProperty("when", `when`)
        add("model", model)
    }
}
