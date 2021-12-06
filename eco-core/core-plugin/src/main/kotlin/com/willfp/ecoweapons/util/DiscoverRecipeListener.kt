package com.willfp.ecoweapons.util

import com.willfp.eco.core.EcoPlugin
import org.bukkit.Bukkit
import org.bukkit.Keyed
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class DiscoverRecipeListener(private val plugin: EcoPlugin) : Listener {
    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (plugin.configYml.getBool("discover-recipes")) {
            Bukkit.getServer().recipeIterator().forEachRemaining {
                if (it is Keyed) {
                    val key = it.key
                    if (key.namespace == plugin.name.lowercase()) {
                        if (!key.key.contains("displayed")) {
                            player.discoverRecipe(key)
                        }
                    }
                }
            }
        }
    }
}