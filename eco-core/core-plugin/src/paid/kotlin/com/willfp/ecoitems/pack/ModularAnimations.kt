package com.willfp.ecoitems.pack

import com.google.gson.JsonObject
import com.willfp.ecoitems.items.EcoItem
import com.willfp.ecoitems.materials.ItemAnimation
import com.willfp.ecoitems.materials.Materials
import com.willfp.ecoitems.materials.ModularItemService
import com.willfp.ecoitems.pack.ModularPoses.idlePose
import com.willfp.ecoitems.pack.ModularPoses.parentOf
import com.willfp.ecoitems.pack.ModularPoses.poses
import com.willfp.ecoitems.pack.ModularPoses.usePose

/** Builds the vanilla bow, crossbow, shield and trident model trees around a modular item's state models. */
internal object ModularAnimations {
    private val BOW_THRESHOLDS = listOf(0.65, 0.9)
    private val CROSSBOW_THRESHOLDS = listOf(0.58, 1.0)
    private const val BOW_SCALE = 0.05

    /**
     * Writes each drawn texture of an animated item again with each of the animation's poses as parent, so it is
     * held like a bow, crossbow, shield or trident. Kept apart from the handheld models other items share.
     */
    fun writePosedModels(entries: MutableMap<String, ByteArray>, items: List<EcoItem>, materialIds: List<String>) {
        ModularPoses.writeOwnPoses(entries, items)
        for ((texture, pose) in items.flatMap { posedTextures(it) }.distinct()) {
            val parent = parentOf(pose)
            entries[generatedModel(texture, pose = pose).entry(root = "models", extension = "json")] =
                modelJson(texture.key, parent)
            for (id in materialIds) {
                entries[generatedModel(texture, id, pose).entry(root = "models", extension = "json")] =
                    modelJson("${texture.key}_$id", parent)
            }
        }
        for ((texture, pose) in items.flatMap { posedMaterialOverlays(it) }.distinct()) {
            entries[generatedModel(texture, pose = pose).entry(root = "models", extension = "json")] =
                modelJson(texture = texture.key, parent = parentOf(pose))
        }
    }

    private fun posedMaterialOverlays(item: EcoItem): List<Pair<PackLocation, String>> {
        val modular = item.modular
        val poses = modular?.animation?.poses() ?: return emptyList()
        val materials = Materials.values()
        return modular.layers.asSequence()
            .flatMap { slotId -> materials.flatMap { it.overlaysIn(slotId) } }
            .mapNotNull { PackLocation.parse(it) }
            .flatMap { texture -> poses.map { texture to it } }
            .toList()
    }

    private fun posedTextures(item: EcoItem): List<Pair<PackLocation, String>> {
        val modular = item.modular
        val poses = modular?.animation?.poses() ?: return emptyList()
        val drawn = modular.layers
            .mapNotNull { modular.slots[it] }
            .mapNotNull { ModularItemService.partItemForShape(it) }
        return (drawn.flatMap { it.allPartTextures() } + item.allOverlayTextures())
            .flatMap { texture -> poses.map { texture to it } }
    }

    /**
     * The model for [animation], where [model] gives the composite for a state (null for idle) drawn in a pose
     * (null for the shared handheld models).
     */
    fun modelTree(animation: ItemAnimation?, model: (String?, String?) -> JsonObject): JsonObject {
        if (animation == null) {
            return model(null, null)
        }
        val idle = model(null, animation.idlePose())
        val inPose: (String?) -> JsonObject = { state -> model(state, animation.usePose()) }
        return when (animation) {
            ItemAnimation.BOW -> whileUsing(
                idle = idle,
                using = pulling(
                    inPose,
                    property = "minecraft:use_duration",
                    thresholds = BOW_THRESHOLDS,
                    scale = BOW_SCALE
                )
            )
            ItemAnimation.CROSSBOW -> crossbow(idle, inPose)
            ItemAnimation.SHIELD -> whileUsing(idle = idle, using = inPose("blocking"))
            ItemAnimation.TRIDENT -> whileUsing(idle = idle, using = inPose("throwing"))
        }
    }

    private fun crossbow(idle: JsonObject, model: (String?) -> JsonObject): JsonObject = jsonObject(
        "type" to "minecraft:select",
        "property" to "minecraft:charge_type",
        "cases" to jsonArray(
            listOf("arrow", "rocket").map { charge -> jsonObject("when" to charge, "model" to model(charge)) }
        ),
        "fallback" to whileUsing(
            idle = idle,
            using = pulling(model, property = "minecraft:crossbow/pull", thresholds = CROSSBOW_THRESHOLDS, scale = null)
        )
    )

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

    /** Held transforms. The sprite's back (+z) faces the holder, where a 3D part such as a strap sits. */
}
