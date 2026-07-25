package com.willfp.ecoitems.items

import com.willfp.eco.core.config.config
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.config.readConfig
import com.willfp.ecoitems.nms.toPlainValues
import com.willfp.ecoitems.plugin

/**
 * Item templates: any _-prefixed yml under items/ can be referenced from an
 * item config with `template: <name>` (the file name without the underscore,
 * e.g. `template: sword_base` for items/_sword_base.yml).
 *
 * Templates merge underneath the item's own config, so items only write the
 * keys that differ. `template:` accepts a list (later entries win), and
 * templates can themselves extend other templates.
 */
object ItemTemplates {
    private var templates = emptyMap<String, Config>()

    internal fun load() {
        templates = plugin.dataFolder.resolve("items")
            .walk()
            .filter { it.isFile && it.name.endsWith(".yml") && it.nameWithoutExtension.startsWith("_") }
            .associate { it.nameWithoutExtension.trimStart('_') to it.readConfig() }
    }

    internal fun resolve(id: String, config: Config): Config {
        val names = templateNames(config)
        if (names.isEmpty()) {
            return config
        }

        var values = emptyMap<String, Any?>()
        for (name in names) {
            values = merge(values, resolveTemplate(id, name, mutableSetOf()))
        }
        values = merge(values, config.toPlainValues())

        return buildConfig(values - "template")
    }

    private fun buildConfig(values: Map<String, Any?>): Config = config {
        for ((key, value) in values) {
            key to value.asConfigValue()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Any?.asConfigValue(): Any? = when (this) {
        is Map<*, *> -> buildConfig(this as Map<String, Any?>)
        is List<*> -> this.map { it.asConfigValue() }
        else -> this
    }

    private fun resolveTemplate(itemId: String, name: String, seen: MutableSet<String>): Map<String, Any?> {
        val template = templates[name]

        if (template == null) {
            plugin.logger.warning("Item $itemId references unknown template '$name' (expected items/_$name.yml)")
            return emptyMap()
        }

        if (!seen.add(name)) {
            plugin.logger.warning("Item $itemId has a circular template reference through '$name'")
            return emptyMap()
        }

        var values = emptyMap<String, Any?>()
        for (parent in templateNames(template)) {
            values = merge(values, resolveTemplate(itemId, parent, seen))
        }

        return merge(values, template.toPlainValues()) - "template"
    }

    private fun templateNames(config: Config): List<String> {
        if (!config.has("template")) {
            return emptyList()
        }

        val names = config.getStrings("template")
        return names.ifEmpty { listOfNotNull(config.getStringOrNull("template")) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun merge(base: Map<String, Any?>, over: Map<String, Any?>): Map<String, Any?> {
        val merged = base.toMutableMap()

        for ((key, value) in over) {
            val existing = merged[key]
            merged[key] = if (existing is Map<*, *> && value is Map<*, *>) {
                merge(existing as Map<String, Any?>, value as Map<String, Any?>)
            } else {
                value
            }
        }

        return merged
    }
}
