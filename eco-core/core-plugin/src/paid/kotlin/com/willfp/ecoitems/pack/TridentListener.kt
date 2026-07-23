package com.willfp.ecoitems.pack

import com.willfp.ecoitems.items.ecoItem
import com.willfp.ecoitems.nms.ItemComponentsProxy
import com.willfp.ecoitems.plugin
import org.bukkit.entity.Trident
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent

/**
 * Custom trident models: there's no client-side "is thrown" property, so a
 * thrown trident's item_model component is swapped to the item's throwing
 * definition in flight, and restored when it lands (so pickup returns the
 * item with its normal model).
 *
 * Reading the projectile's item needs Paper; on Spigot thrown tridents just
 * keep the held model.
 */
object TridentListener : Listener {
    /** Item id -> throwing definition key, rebuilt each reload. */
    private var throwingModels = emptyMap<String, String>()

    fun update(assets: List<ItemPackAsset>) {
        throwingModels = assets
            .filter { it.throwing != null }
            .associate { it.id to "ecoitems:${it.id}_throwing" }
    }

    @EventHandler(ignoreCancelled = true)
    fun onLaunch(event: ProjectileLaunchEvent) {
        val trident = event.entity as? Trident ?: return

        swapModel(trident) { id -> throwingModels[id] }
    }

    @EventHandler(ignoreCancelled = true)
    fun onHit(event: ProjectileHitEvent) {
        val trident = event.entity as? Trident ?: return

        // Back to the item's own definition once landed.
        swapModel(trident) { id -> if (id in throwingModels) "ecoitems:$id" else null }
    }

    private fun swapModel(trident: Trident, model: (String) -> String?) {
        val stack = runCatching { trident.itemStack }.getOrNull() ?: return
        val id = stack.ecoItem?.id?.key ?: return
        val key = model(id) ?: return

        val result = plugin.getProxy(ItemComponentsProxy::class.java)
            .withComponents(stack, mapOf("minecraft:item_model" to key))

        for (error in result.errors) {
            plugin.logger.warning("Could not swap thrown trident model for $id: $error")
        }

        runCatching { trident.itemStack = result.item }
    }
}
