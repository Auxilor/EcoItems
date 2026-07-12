package com.willfp.ecoitems.paintings

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.registry.KRegistrable
import com.willfp.ecoitems.plugin
import java.util.Objects

class Painting(
    override val id: String,
    val config: Config
) : KRegistrable {
    /**
     * The texture, a [ns:]path relative to textures/painting/, defaulting to
     * the painting's id. Also the registry asset id.
     */
    val texture: String = config.getStringOrNull("texture") ?: id

    /** Size in blocks (each block is 16 texture pixels). */
    val width = (config.getIntOrNull("width") ?: 1).coerceIn(1, 16)
    val height = (config.getIntOrNull("height") ?: 1).coerceIn(1, 16)

    val title: String? = config.getStringOrNull("title")
    val author: String? = config.getStringOrNull("author")

    init {
        if ((config.getIntOrNull("width") ?: 1) !in 1..16 || (config.getIntOrNull("height") ?: 1) !in 1..16) {
            plugin.logger.warning("Painting $id's size must be 1-16 blocks per side; clamped")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Painting) {
            return false
        }

        return this.id == other.id
    }

    override fun hashCode(): Int {
        return Objects.hash(this.id)
    }

    override fun toString(): String {
        return "Painting{$id}"
    }
}
