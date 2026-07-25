package com.willfp.ecoitems.pack

import com.willfp.ecoitems.EcoItemsPlugin
import java.io.File

/**
 * A resource location in the pack, written in configs as "[namespace:]path"
 * with the namespace defaulting to ecoitems. Paths are relative to the asset
 * root they're used with (textures/, models/, sounds/), extensionless.
 */
data class PackLocation(
    val namespace: String,
    val path: String
) {
    val key: String
        get() = "$namespace:$path"

    /** The backing file in the pack folder, e.g. pack/assets/<ns>/textures/<path>.png. */
    fun file(plugin: EcoItemsPlugin, root: String, extension: String): File =
        plugin.dataFolder.resolve("pack/assets/$namespace/$root/$path.$extension")

    /** The pack entry path, e.g. assets/<ns>/models/<path>.json. */
    fun entry(root: String, extension: String): String =
        "assets/$namespace/$root/$path.$extension"

    override fun toString(): String = key

    companion object {
        private val NAMESPACE_REGEX = Regex("[a-z0-9_.-]+")
        private val PATH_REGEX = Regex("[a-z0-9_./-]+")

        /** Parses "[ns:]path", or null if it isn't a valid location. */
        fun parse(value: String): PackLocation? {
            val namespace = if (":" in value) value.substringBefore(':') else "ecoitems"
            val path = value.substringAfter(':')

            if (!namespace.matches(NAMESPACE_REGEX) || !path.matches(PATH_REGEX)) {
                return null
            }

            return PackLocation(namespace, path)
        }
    }
}
