package com.willfp.ecoitems.pack

import com.google.gson.JsonObject
import com.willfp.ecoitems.items.EcoItem
import com.willfp.ecoitems.materials.ItemAnimation
import com.willfp.ecoitems.materials.ModularItemService

/** Builds the vanilla bow and crossbow model trees around a modular item's state models. */
internal object ModularAnimations {
    private val BOW_THRESHOLDS = listOf(0.65, 0.9)
    private val CROSSBOW_THRESHOLDS = listOf(0.58, 1.0)
    private const val BOW_SCALE = 0.05

    /**
     * Writes each drawn texture of an animated item again with the animation's vanilla model as parent, so it is
     * held like a bow or crossbow. Kept apart from the handheld models other items share.
     */
    fun writePosedModels(entries: MutableMap<String, ByteArray>, items: List<EcoItem>, materialIds: List<String>) {
        for ((texture, pose) in items.flatMap { posedTextures(it) }.distinct()) {
            val parent = "minecraft:item/$pose"
            entries[generatedModel(texture, pose = pose).entry(root = "models", extension = "json")] =
                modelJson(texture.key, parent)
            for (id in materialIds) {
                entries[generatedModel(texture, id, pose).entry(root = "models", extension = "json")] =
                    modelJson("${texture.key}_$id", parent)
            }
        }
    }

    private fun posedTextures(item: EcoItem): List<Pair<PackLocation, String>> {
        val modular = item.modular
        val pose = modular?.animation?.id ?: return emptyList()
        val drawn = modular.layers
            .mapNotNull { modular.slots[it] }
            .mapNotNull { ModularItemService.partItemForShape(it) }
        return (drawn.flatMap { it.allPartTextures() } + item.slotTextures() + item.allOverlayTextures())
            .map { it to pose }
    }

    /** The model for [animation], where [model] gives the composite for a state, or null for idle. */
    fun modelTree(animation: ItemAnimation?, model: (String?) -> JsonObject): JsonObject = when (animation) {
        null -> model(null)
        ItemAnimation.BOW -> whileUsing(
            idle = model(null),
            using = pulling(model, property = "minecraft:use_duration", thresholds = BOW_THRESHOLDS, scale = BOW_SCALE)
        )
        ItemAnimation.CROSSBOW -> jsonObject(
            "type" to "minecraft:select",
            "property" to "minecraft:charge_type",
            "cases" to jsonArray(
                listOf("arrow", "rocket").map { charge -> jsonObject("when" to charge, "model" to model(charge)) }
            ),
            "fallback" to whileUsing(
                idle = model(null),
                using = pulling(
                    model,
                    property = "minecraft:crossbow/pull",
                    thresholds = CROSSBOW_THRESHOLDS,
                    scale = null
                )
            )
        )
    }

    private fun whileUsing(idle: JsonObject, using: JsonObject): JsonObject = jsonObject(
        "type" to "minecraft:condition",
        "property" to "minecraft:using_item",
        "on_false" to idle,
        "on_true" to using
    )

    private fun pulling(
        model: (String?) -> JsonObject,
        property: String,
        thresholds: List<Double>,
        scale: Double?
    ): JsonObject {
        val json = jsonObject(
            "type" to "minecraft:range_dispatch",
            "property" to property,
            "entries" to jsonArray(
                thresholds.mapIndexed { index, threshold ->
                    jsonObject("threshold" to threshold, "model" to model("pulling_${index + 1}"))
                }
            ),
            "fallback" to model("pulling_0")
        )
        if (scale != null) {
            json.addProperty("scale", scale)
        }
        return json
    }
}
