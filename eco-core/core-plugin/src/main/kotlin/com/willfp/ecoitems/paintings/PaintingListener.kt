package com.willfp.ecoitems.paintings

import com.willfp.ecoitems.items.EcoItems
import com.willfp.ecoitems.nms.ItemComponentsProxy
import com.willfp.ecoitems.plugin
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.Painting
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.hanging.HangingBreakEvent
import org.bukkit.inventory.ItemStack

/**
 * Vanilla drops a plain painting item when a painting breaks, losing the
 * variant - for our registered variants, drop the matching custom item (or a
 * painting that keeps the variant) instead.
 */
object PaintingListener : Listener {
    @EventHandler(ignoreCancelled = true)
    fun onBreak(event: HangingBreakEvent) {
        val painting = event.entity as? Painting ?: return
        val key = painting.art.key
        if (key.namespace != "ecoitems" || Paintings[key.key] == null) {
            return
        }

        event.isCancelled = true
        painting.remove()
        painting.world.playSound(painting.location, Sound.ENTITY_PAINTING_BREAK, 1f, 1f)

        val remover = (event as? HangingBreakByEntityEvent)?.remover
        if ((remover as? Player)?.gameMode == GameMode.CREATIVE) {
            return
        }

        painting.world.dropItemNaturally(painting.location, itemFor(key))
    }

    private fun itemFor(variant: NamespacedKey): ItemStack {
        for (item in EcoItems.values()) {
            val components = item.config.getSubsection("item").getSubsection("components")
            val matches = components.getKeys(false).any {
                it.removePrefix("minecraft:") == "painting/variant" &&
                    components.getStringOrNull(it) == variant.toString()
            }
            if (matches) {
                return item.itemStack
            }
        }

        return plugin.getProxy(ItemComponentsProxy::class.java)
            .withComponents(
                ItemStack(Material.PAINTING),
                mapOf("minecraft:painting/variant" to variant.toString())
            )
            .item
    }
}
