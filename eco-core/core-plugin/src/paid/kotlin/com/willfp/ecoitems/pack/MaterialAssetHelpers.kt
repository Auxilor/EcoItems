package com.willfp.ecoitems.pack

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.materials.MaterialPalettes
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

internal const val GREY_STEP = 0x010101

private const val OPAQUE = 0xFF000000.toInt()
private const val RGB_MASK = 0xFFFFFF

internal val materialGson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

internal fun paletteImage(colors: List<Int>): ByteArray {
    val image = BufferedImage(colors.size, 1, BufferedImage.TYPE_INT_ARGB)
    colors.forEachIndexed { x, rgb -> image.setRGB(x, 0, OPAQUE or rgb) }
    return ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
}

internal fun warnAboutStrayGreys(plugin: EcoItemsPlugin, texture: PackLocation) {
    val file = texture.file(plugin, root = "textures", extension = "png")
    if (!file.exists()) {
        plugin.logger.warning(
            "Part texture pack/${texture.entry(root = "textures", extension = "png")} does not exist"
        )
    } else if (strayColours(file) > 0) {
        plugin.logger.warning(
            "Part texture ${texture.key} uses colours outside the material grey ramp " +
                "${MaterialPalettes.RAMP}; they will not be recoloured"
        )
    }
}

private fun strayColours(file: File): Int {
    val image = runCatching { ImageIO.read(file) }.getOrNull() ?: return 0
    val ramp = MaterialPalettes.RAMP.map { it * GREY_STEP }.toSet()

    return (0 until image.width).asSequence()
        .flatMap { x -> (0 until image.height).asSequence().map { y -> image.getRGB(x, y) } }
        .filter { it ushr Int.SIZE_BITS - Byte.SIZE_BITS != 0 }
        .map { it and RGB_MASK }
        .filterNot { it in ramp }
        .distinct()
        .count()
}

internal fun jsonObject(vararg properties: Pair<String, Any>): JsonObject {
    val json = JsonObject()
    for ((key, value) in properties) {
        json.add(key, toJson(value))
    }
    return json
}

internal fun jsonArray(values: List<Any>): JsonArray {
    val json = JsonArray()
    values.forEach { json.add(toJson(it)) }
    return json
}

private fun toJson(value: Any): JsonElement = if (value is JsonElement) value else materialGson.toJsonTree(value)
