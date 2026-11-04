package com.willfp.ecoitems.pack

import com.google.gson.JsonObject
import com.willfp.ecoitems.items.EcoItem
import com.willfp.ecoitems.materials.ItemAnimation

/**
 * How an animated item is held in each state. Bows and crossbows use the vanilla models. Vanilla shields and
 * tridents are 3D entity models whose transforms don't fit a flat sprite, so their poses are written as
 * `ecoitems:item/pose/<pose>`. A shield's back (+z) faces the holder, where a 3D part such as a strap sits,
 * and its inventory icon shows the front.
 */
internal object ModularPoses {
    private val SHIELD_GUI = transform(rotation = listOf(0, 180, 0), translation = listOf(0, 0, 0), scale = 1.0)

    private val OWN_POSES: Map<String, JsonObject> = mapOf(
        "shield" to display(
            thirdPerson = transform(rotation = listOf(0, -90, 0), translation = listOf(3.5, -2, 4), scale = 1.3),
            firstPerson = transform(rotation = listOf(0, 0, -5), translation = listOf(4.5, -6.5, -7.7), scale = 1.8),
            gui = SHIELD_GUI
        ),
        "shield_blocking" to display(
            thirdPerson = transform(rotation = listOf(36, -5, 10), translation = listOf(2.3, -1.2, -1.3), scale = 1.2),
            firstPerson = transform(rotation = listOf(0, 0, 5), translation = listOf(-1.5, -3, -8.7), scale = 1.8),
            gui = SHIELD_GUI
        ),
        "trident_throwing" to display(
            thirdPerson = transform(rotation = listOf(0, -90, -125), translation = listOf(0, -4, 0.5), scale = 0.85),
            firstPerson = transform(rotation = listOf(0, -90, 25), translation = listOf(1.13, 3.2, 1.13), scale = 0.68)
        )
    )

    /** The pose while idle. Null is the shared handheld model. */
    fun ItemAnimation.idlePose(): String? = if (this == ItemAnimation.TRIDENT) null else id

    /** The pose while the item is used: drawn, blocking or thrown. */
    fun ItemAnimation.usePose(): String = when (this) {
        ItemAnimation.BOW, ItemAnimation.CROSSBOW -> id
        ItemAnimation.SHIELD -> "shield_blocking"
        ItemAnimation.TRIDENT -> "trident_throwing"
    }

    fun ItemAnimation.poses(): List<String> = listOfNotNull(idlePose(), usePose()).distinct()

    /** The model a texture drawn in [pose] uses as parent, for its held transforms. Null is the shared handheld. */
    fun parentOf(pose: String?): String = when (pose) {
        null -> HANDHELD_PARENT
        in OWN_POSES -> "ecoitems:item/pose/$pose"
        else -> "minecraft:item/$pose"
    }

    /** Writes the poses the items use that vanilla has no fitting model for. */
    fun writeOwnPoses(entries: MutableMap<String, ByteArray>, items: List<EcoItem>) {
        val usedPoses = items.flatMap { it.modular?.animation?.poses().orEmpty() }.toSet()
        for ((pose, display) in OWN_POSES.filterKeys { it in usedPoses }) {
            val location = PackLocation(namespace = "ecoitems", path = "item/pose/$pose")
            entries[location.entry(root = "models", extension = "json")] = materialGson.toJson(
                jsonObject("parent" to "minecraft:item/generated", "display" to display)
            ).encodeToByteArray()
        }
    }

    private fun display(thirdPerson: JsonObject, firstPerson: JsonObject, gui: JsonObject? = null): JsonObject {
        val json = jsonObject("thirdperson_righthand" to thirdPerson, "firstperson_righthand" to firstPerson)
        if (gui != null) {
            json.add("gui", gui)
        }
        return json
    }

    private fun transform(rotation: List<Number>, translation: List<Number>, scale: Double): JsonObject = jsonObject(
        "rotation" to rotation,
        "translation" to translation,
        "scale" to listOf(scale, scale, scale)
    )
}
