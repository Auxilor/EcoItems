package com.willfp.ecoitems.pack

import com.willfp.ecoitems.BuildConfig
import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.plugin
import org.bukkit.event.Listener

/**
 * The resource pack system, only present in paid builds.
 */
interface PackFeature {
    fun listeners(plugin: EcoItemsPlugin): List<Listener>

    fun handleEnable(plugin: EcoItemsPlugin) {}

    fun handleReload(plugin: EcoItemsPlugin)

    fun handleDisable(plugin: EcoItemsPlugin)
}

object PackFeatures {
    // The implementation lives in the paid source set, so free builds can't
    // reference it directly; it's looked up reflectively instead.
    val instance: PackFeature? by lazy {
        if (BuildConfig.FREE_VERSION) {
            null
        } else {
            try {
                Class.forName("com.willfp.ecoitems.pack.EcoItemsPackFeature")
                    .getField("INSTANCE")
                    .get(null) as PackFeature
            } catch (e: Exception) {
                plugin.logger.severe("Failed to load the resource pack system: $e")
                null
            }
        }
    }
}
