package com.willfp.ecoitems.items

import com.willfp.eco.util.asAudience
import com.willfp.eco.util.formatEco
import com.willfp.eco.util.toComponent
import com.willfp.ecoitems.plugin
import org.bukkit.entity.AbstractArrow
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable

/**
 * Items with keep-when-broken stop at their last point of durability instead of breaking. While
 * broken they can't break blocks and their attacks deal one damage, until they are repaired.
 */
object KeepWhenBrokenListener : Listener {
    private const val BROKEN_ATTACK_DAMAGE = 1.0

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onItemDamage(event: PlayerItemDamageEvent) {
        val damageLeft = event.item.damageLeft() ?: return

        if (event.damage >= damageLeft) {
            event.damage = damageLeft.coerceAtLeast(0)
            event.isCancelled = event.damage == 0
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        if (event.player.inventory.itemInMainHand.isBroken()) {
            event.isCancelled = true
            event.player.asAudience().sendActionBar(
                plugin.langYml.getString("messages.item-broken").formatEco().toComponent()
            )
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onAttack(event: EntityDamageByEntityEvent) {
        val weapon = when (val damager = event.damager) {
            is Player -> damager.inventory.itemInMainHand
            is AbstractArrow -> damager.weapon
            else -> null
        }

        if (weapon?.isBroken() == true) {
            event.damage = BROKEN_ATTACK_DAMAGE
        }
    }

    private fun ItemStack.isBroken(): Boolean = damageLeft()?.let { it <= 0 } == true

    private fun ItemStack.damageLeft(): Int? {
        val meta = itemMeta as? Damageable
        if (ecoItem?.keepsWhenBroken != true || meta == null) {
            return null
        }

        val maxDamage = if (meta.hasMaxDamage()) meta.maxDamage else type.maxDurability.toInt()
        return (maxDamage - 1 - meta.damage).takeIf { maxDamage > 0 }
    }
}
