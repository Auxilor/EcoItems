package com.willfp.ecoitems.items

import com.willfp.ecoitems.plugin
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable

/**
 * Rebuilds items in player inventories from their current configs on join,
 * on pickup, and after a reload, so config changes reach items that are
 * already in circulation.
 *
 * Durability, anvil renames, extra enchantments, and other plugins'
 * persistent data survive the update.
 */
object ItemUpdater : Listener {
    private val enabled: Boolean
        get() = plugin.configYml.getBool("auto-update-items")

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        if (!enabled) {
            return
        }

        update(event.player.inventory)
        update(event.player.enderChest)
    }

    @EventHandler
    fun onPickup(event: EntityPickupItemEvent) {
        if (!enabled || event.entity !is Player) {
            return
        }

        val updated = updated(event.item.itemStack) ?: return
        event.item.itemStack = updated
    }

    internal fun updateOnlinePlayers() {
        if (!enabled) {
            return
        }

        for (player in Bukkit.getOnlinePlayers()) {
            update(player.inventory)
            update(player.enderChest)
        }
    }

    private fun update(inventory: Inventory) {
        for (slot in 0 until inventory.size) {
            val updated = updated(inventory.getItem(slot) ?: continue) ?: continue
            inventory.setItem(slot, updated)
        }
    }

    /** The rebuilt stack, or null if [stack] is not an EcoItem or already current. */
    @Suppress("DEPRECATION")
    private fun updated(stack: ItemStack): ItemStack? {
        val item = stack.ecoItem ?: return null

        val fresh = item.itemStack
        fresh.amount = stack.amount

        val oldMeta = stack.itemMeta ?: return null
        val freshMeta = fresh.itemMeta ?: return null

        if (oldMeta is Damageable && freshMeta is Damageable && oldMeta.hasDamage()) {
            freshMeta.damage = oldMeta.damage
        }

        if (oldMeta.hasDisplayName() && !freshMeta.hasDisplayName()) {
            freshMeta.setDisplayName(oldMeta.displayName)
        }

        // replace = false: foreign keys copy over, ours stay authoritative.
        oldMeta.persistentDataContainer.copyTo(freshMeta.persistentDataContainer, false)

        // Player-added enchantments survive; config levels win on overlap.
        for ((enchantment, level) in oldMeta.enchants) {
            if (!freshMeta.hasEnchant(enchantment)) {
                freshMeta.addEnchant(enchantment, level, true)
            }
        }

        fresh.itemMeta = freshMeta

        return if (fresh == stack) null else fresh
    }
}
