package com.willfp.ecoitems.nms

import com.willfp.eco.core.config.ConfigType
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.config.readConfig
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
 * Convert a config section to plain values (maps, lists, scalars), e.g. for
 * [ItemComponentsProxy.withComponents] or template merging.
 *
 * Roundtrips through plaintext because wrapper configs (e.g. libreforge's
 * separator-ambivalent wrapper) don't implement toMap and must not rewrite
 * component keys anyway.
 */
fun Config.toPlainValues(): Map<String, Any?> =
    readConfig(toPlaintext(), ConfigType.YAML).toMap()
        .mapValues { (_, value) -> value.toPlainValue() }

private fun Any?.toPlainValue(): Any? = when (this) {
    is Config -> this.toPlainValues()
    is Iterable<*> -> this.map { it.toPlainValue() }
    else -> this
}
