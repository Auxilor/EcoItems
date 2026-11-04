package com.willfp.ecoitems.pack

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.items.EcoItem
import com.willfp.ecoitems.pack.ModularPoses.poses

/** The 3D model the part is drawn with in a modular item, or null for a flat sprite. */
internal fun EcoItem.partModel(): PackLocation? = part?.model?.let { PackLocation.parse(it) }

/**
 * Parts drawn in 3D, such as a shield strap. Each part model is written again per texture, material and pose,
 * with the recoloured texture as #layer0 and the pose's model as parent: the child's elements replace the
 * generated sprite, the parent keeps the held transforms.
 */
internal object PartShapes {
    /** The elements of each part's model. A model that can't be read is skipped, so the part stays flat. */
    fun read(plugin: EcoItemsPlugin, items: List<EcoItem>): Map<PackLocation, JsonArray> =
        items.asSequence().mapNotNull { it.partModel() }.distinct().mapNotNull { shape ->
            val elements = runCatching {
                val file = shape.file(plugin, root = "models", extension = "json")
                JsonParser.parseString(file.readText()).asJsonObject.getAsJsonArray("elements")
            }.getOrNull()
            if (elements == null) {
                plugin.logger.warning(
                    "Part model pack/${shape.entry(root = "models", extension = "json")} is skipped: " +
                        "it needs an elements list"
                )
            }
            elements?.let { shape to it }
        }.toMap()

    fun write(
        entries: MutableMap<String, ByteArray>,
        items: List<EcoItem>,
        shapes: Map<PackLocation, JsonArray>,
        materialIds: List<String>
    ) {
        val poses = listOf<String?>(null) + items.flatMap { it.modular?.animation?.poses().orEmpty() }.distinct()
        val ids = listOf<String?>(null) + materialIds
        val drawn = items.mapNotNull { part -> part.partModel()?.takeIf { it in shapes }?.let { part to it } }
        for ((part, shape) in drawn) {
            val elements = shapes.getValue(shape)
            for (texture in part.allPartTextures()) {
                for ((pose, id) in poses.flatMap { pose -> ids.map { pose to it } }) {
                    val layer = texture.key + id?.let { "_$it" }.orEmpty()
                    entries[shapedModel(texture, shape, id, pose).entry(root = "models", extension = "json")] =
                        materialGson.toJson(
                            jsonObject(
                                "parent" to ModularPoses.parentOf(pose),
                                "textures" to jsonObject("layer0" to layer, "particle" to layer),
                                "elements" to elements
                            )
                        ).encodeToByteArray()
                }
            }
        }
    }
}
