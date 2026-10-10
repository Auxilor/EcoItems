package com.willfp.ecoitems.api

import org.bukkit.inventory.ItemStack

/** One EcoItems fishing catch: the item that replaces the vanilla catch, and its XP. */
data class FishingRoll(val item: ItemStack, val experience: Int)
