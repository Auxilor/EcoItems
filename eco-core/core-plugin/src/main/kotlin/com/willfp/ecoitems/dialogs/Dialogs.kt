package com.willfp.ecoitems.dialogs

import com.google.common.collect.ImmutableList
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.registry.Registry
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.ConfigCategory
import com.willfp.libreforge.separatorAmbivalent

object Dialogs : ConfigCategory("dialog", "dialogs") {
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

    override fun clear(plugin: LibreforgePlugin) {
        registry.clear()
    }

    override fun acceptConfig(plugin: LibreforgePlugin, id: String, config: Config) {
        registry.register(EcoDialog(id, config.separatorAmbivalent()))
    }
}
