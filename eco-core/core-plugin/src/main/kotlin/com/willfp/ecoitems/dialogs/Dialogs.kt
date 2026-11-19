package com.willfp.ecoitems.dialogs

import com.google.common.collect.ImmutableList
import wiki.ecohub.config.Config
import wiki.ecohub.config.category.NativeConfigCategory
import wiki.ecohub.config.section.separatorAmbivalent
import wiki.ecohub.core.plugin.EcoPlugin
import wiki.ecohub.core.registry.Registry

object Dialogs : NativeConfigCategory("dialogs", "dialog") {
    private val registry = Registry<EcoDialog>()

    fun getByID(id: String?): EcoDialog? {
        if (id == null) {
            return null
        }

        return registry[id]
    }

    fun values(): List<EcoDialog> {
        return ImmutableList.copyOf(registry.values())
    }

    override fun clear(plugin: EcoPlugin) {
        registry.clear()
    }

    override fun acceptConfig(plugin: EcoPlugin, id: String, config: Config) {
        registry.register(EcoDialog(id, config.separatorAmbivalent()))
    }
}
