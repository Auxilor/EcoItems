package com.willfp.ecoitems.loots

import com.willfp.libreforge.drops.DropContribution
import com.willfp.libreforge.drops.DropContributor
import com.willfp.libreforge.triggers.event.DropCause
import com.willfp.libreforge.triggers.event.DropContext
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * Injects loot configs into vanilla gameplay: block breaks, mob kills, and
 * fishing. Targets are resolved through eco's block lookup.
 *
 * Loot is contributed to libreforge's drop pipeline rather than dropped
 * directly, so it goes through the same effects as any other drop -
 * multiply_drops, cancel_drops, telekinesis, and the rest - and the drop
 * trigger still fires only once per break, kill, or catch.
 */
object LootContributor : DropContributor {
    override fun contribute(
        cause: DropCause,
        context: DropContext,
        location: Location
    ): DropContribution {
        val player = context.player ?: return DropContribution.EMPTY

        if (player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR) {
            return DropContribution.EMPTY
        }

        val items = mutableListOf<ItemStack>()
        var xp = 0

        for (loot in Loots.values()) {
            if (!loot.rolls(cause, context, location, player)) {
                continue
            }

            val fortune = if (loot.fortune && cause == DropCause.BLOCK) {
                context.tool?.getEnchantmentLevel(Enchantment.FORTUNE) ?: 0
            } else {
                0
            }

            items += rollItems(loot, fortune)
            if (!loot.xp.isEmpty()) {
                xp += loot.xp.random().coerceAtLeast(0)
            }
        }

        return DropContribution(items, xp)
    }

    private fun Loot.rolls(
        cause: DropCause,
        context: DropContext,
        location: Location,
        player: Player
    ): Boolean = when (cause) {
        DropCause.BLOCK -> context.block?.let { rollsForBlock(it, player) } ?: false
        DropCause.ENTITY -> context.entity?.let { rollsForEntity(it, player) } ?: false
        // Fishing loot replaces the catch rather than adding to it, which a
        // contribution can't express - see LootFishingListener.
        else -> false
    }

    internal fun rollItems(loot: Loot, fortune: Int): List<ItemStack> {
        val items = mutableListOf<ItemStack>()

        for (drop in loot.items) {
            if (Random.nextDouble() > drop.chance) {
                continue
            }

            val stack = drop.toItemStack() ?: continue

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
