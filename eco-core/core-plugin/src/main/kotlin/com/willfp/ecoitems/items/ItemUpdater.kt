package com.willfp.ecoitems.items

import com.willfp.eco.core.fast.fast
import com.willfp.ecoitems.plugin
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ArmorMeta
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

/**
 * Rebuilds items in player inventories from their current configs on join,
 * on pickup, and after a reload, so config changes reach items that are
 * already in circulation.
 *
 * Durability, anvil renames/relores, armor trims, extra enchantments, and
 * other plugins' persistent data survive the update.
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

        // A rename is only "custom" if it differs from the name the config had
        // when this stack was last built - otherwise it's just the config default,
        // and the (possibly changed) fresh default should apply. Same idea for lore:
        // if it doesn't match the lore this stack was built with, something else
        // (another plugin, or a player) touched it, so leave it alone.
        val nameBaseline = oldMeta.persistentDataContainer.get(baseDisplayNameKey, PersistentDataType.STRING)
        if (oldMeta.hasDisplayName() && oldMeta.displayName != nameBaseline) {
            freshMeta.setDisplayName(oldMeta.displayName)
        }

        // Lore is compared and carried over as components. The legacy string form
        // cannot represent everything a component can, so going through it both
        // reports unchanged lore as customised and rewrites it into a shape that
        // no longer stacks with a freshly built item.
        val oldLore = stack.fast().loreComponents
        val customLore = if (oldLore.isEmpty() || oldLore.isConfigLore(oldMeta, stack, fresh)) {
            null
        } else {
            oldLore
        }

        // Same idea again for trims: configs can set them via item components, but
        // a smithing table can also apply one, so only overwrite an untouched trim.
        if (oldMeta is ArmorMeta && freshMeta is ArmorMeta) {
            val trimBaseline = oldMeta.persistentDataContainer.get(baseTrimKey, PersistentDataType.STRING)
            val oldTrim = oldMeta.trim
            if (oldTrim != null && oldTrim.encoded() != trimBaseline) {
                freshMeta.trim = oldTrim
            }
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

        if (customLore != null) {
            fresh.fast().loreComponents = customLore
        }

        return if (fresh == stack) null else fresh
    }

    /**
     * Whether this lore is the item's config lore rather than something a player
     * or another plugin set.
     *
     * Items built before lore was tracked as components only have the legacy
     * baseline, and items already rewritten into the legacy shape match neither
     * baseline, so both are also compared through the legacy form - matching
     * there means the lore is the config's, just in a stale representation, and
     * the freshly built lore should replace it.
     */
    private fun List<Component>.isConfigLore(
        oldMeta: ItemMeta,
        stack: ItemStack,
        fresh: ItemStack
    ): Boolean {
        if (this == fresh.fast().loreComponents) {
            return true
        }

        val pdc = oldMeta.persistentDataContainer

        if (encoded() == pdc.get(baseLoreComponentsKey, PersistentDataType.STRING)) {
            return true
        }

        val legacyLore = stack.fast().lore

        return legacyLore == fresh.fast().lore ||
                legacyLore == pdc.get(baseLoreKey, PersistentDataType.STRING)?.split(LORE_SEPARATOR)
    }
}
