package com.willfp.ecoitems.loots

import com.willfp.eco.core.drops.DropQueue
import com.willfp.eco.core.items.Items
import com.willfp.ecoitems.blocks.EcoBlocks
import com.willfp.ecoitems.plugin
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * Injects loot configs into vanilla gameplay: block breaks, mob kills, and
 * fishing. Custom blocks are skipped - they have their own drops.
 */
object LootListener : Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        if (player.gameMode == GameMode.CREATIVE || !event.isDropItems) {
            return
        }
        if (EcoBlocks.at(event.block) != null) {
            return
        }

        val block = event.block
        val target = block.type.name.lowercase()
        val tool = player.inventory.itemInMainHand

        for (loot in Loots.values()) {
            if (loot.type != LootType.BLOCK || !loot.rolls(target, block.world, block.biome, player)) {
                continue
            }

            val fortune = if (loot.fortune) tool.getEnchantmentLevel(Enchantment.FORTUNE) else 0
            DropQueue(player)
                .addItems(rollItems(loot, fortune))
                .addXP(if (loot.xp.isEmpty()) 0 else loot.xp.random().coerceAtLeast(0))
                .setLocation(block.location)
                .push()
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onMobKill(event: EntityDeathEvent) {
        val killer = event.entity.killer ?: return
        val target = event.entity.type.name.lowercase()
        val location = event.entity.location

        for (loot in Loots.values()) {
            if (loot.type != LootType.MOB ||
                !loot.rolls(target, location.world ?: return, location.block.biome, killer)
            ) {
                continue
            }

            event.drops += rollItems(loot, 0)
            if (!loot.xp.isEmpty()) {
                event.droppedExp += loot.xp.random().coerceAtLeast(0)
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onFish(event: PlayerFishEvent) {
        if (event.state != PlayerFishEvent.State.CAUGHT_FISH) {
            return
        }
        val caught = event.caught as? Item ?: return
        val hook = event.hook.location

        for (loot in Loots.values()) {
            if (loot.type != LootType.FISHING ||
                !loot.rolls(null, hook.world ?: return, hook.block.biome, event.player)
            ) {
                continue
            }

            // Fishing replaces the vanilla catch with one rolled item.
            val replacement = rollItems(loot, 0).firstOrNull() ?: continue
            caught.itemStack = replacement
            if (!loot.xp.isEmpty()) {
                event.expToDrop += loot.xp.random().coerceAtLeast(0)
            }
            return
        }
    }

    private fun rollItems(loot: Loot, fortune: Int): List<ItemStack> {
        val items = mutableListOf<ItemStack>()

        for (drop in loot.items) {
            if (Random.nextDouble() > drop.chance) {
                continue
            }

            val stack = Items.lookup(drop.item).item
            if (stack.type == Material.AIR) {
                plugin.logger.warning("Loot ${loot.id} drop '${drop.item}' is not a valid item")
                continue
            }

            var amount = if (drop.amount.isEmpty()) 1 else drop.amount.random()
            if (fortune > 0) {
                amount *= Random.nextInt(1, fortune + 2)
            }

            stack.amount = amount.coerceIn(1, stack.maxStackSize)
            items += stack
        }

        return items
    }
}
