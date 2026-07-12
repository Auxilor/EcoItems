package com.willfp.ecoitems.nms

import com.willfp.eco.core.config.ConfigType
import com.willfp.eco.core.config.TransientConfig
import com.willfp.eco.core.config.interfaces.Config
import org.bukkit.inventory.ItemStack

interface ItemComponentsProxy {
    /**
     * Returns a copy of [item] with [components] applied.
     *
     * Components are parsed with the vanilla codecs, using the same format
     * as item components in commands. Invalid components are skipped, with a
     * human-readable message added to [ComponentResult.errors].
     */
    fun withComponents(item: ItemStack, components: Map<String, Any?>): ComponentResult
}

data class ComponentResult(
    val item: ItemStack,
    val errors: List<String>
)

/**
 * Convert a config section to plain values (maps, lists, scalars) for
 * [ItemComponentsProxy.withComponents].
 *
 * Roundtrips through plaintext because wrapper configs (e.g. libreforge's
 * separator-ambivalent wrapper) don't implement toMap and must not rewrite
 * component keys anyway.
 */
fun Config.toComponentValues(): Map<String, Any?> =
    TransientConfig(toPlaintext(), ConfigType.YAML).toMap()
        .mapValues { (_, value) -> value.toPlainValue() }

private fun Any?.toPlainValue(): Any? = when (this) {
    is Config -> this.toComponentValues()
    is Iterable<*> -> this.map { it.toPlainValue() }
    else -> this
}
