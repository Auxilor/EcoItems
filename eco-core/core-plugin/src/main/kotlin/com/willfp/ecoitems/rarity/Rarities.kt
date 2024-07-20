package com.willfp.ecoitems.rarity

import com.github.benmanes.caffeine.cache.Caffeine
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.fast.FastItemStack
import com.willfp.eco.core.fast.fast
import com.willfp.eco.core.items.HashedItem
import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.items.ecoItem
import com.willfp.ecoitems.plugin
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.RegistrableCategory
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import java.util.Optional
import java.util.concurrent.TimeUnit
import kotlin.jvm.optionals.getOrNull

object Rarities : RegistrableCategory<Rarity>("rarity", "rarities") {
    override fun clear(plugin: LibreforgePlugin) {
        registry.clear()
        cache.invalidateAll()
    }

    override fun acceptConfig(plugin: LibreforgePlugin, id: String, config: Config) {
        registry.register(Rarity(id, config, plugin as EcoItemsPlugin))
    }

    val defaultRarity: Rarity
        get() = Rarities[plugin.configYml.getString("rarity.default")]
            ?: throw IllegalStateException("Invalid default rarity!")
}

private val cache = Caffeine.newBuilder()
    .expireAfterAccess(5, TimeUnit.SECONDS)
    .build<HashedItem, Optional<Rarity>>()

val ItemStack.ecoItemRarity: Rarity?
    get() {
        val nbtRarity = this.nbtRarity
        val configuredRarity = this.ecoItem?.rarity

        val item = HashedItem.of(this)

        return cache.get(item) {
            Optional.ofNullable(
                nbtRarity ?: configuredRarity ?: Rarities.values()
                    .sortedByDescending { it.weight }
                    .firstOrNull { it.matches(this) }
            )
        }.getOrNull()
    }

private val rarityKey = plugin.createNamespacedKey("rarity")

var ItemStack.nbtRarity: Rarity?
    get() {
        return this.fast().nbtRarity
    }
    set(value) {
        this.fast().nbtRarity = value
    }

var FastItemStack.nbtRarity: Rarity?
    get() {
        return this.persistentDataContainer.nbtRarity
    }
    set(value) {
        this.persistentDataContainer.nbtRarity = value
    }

var PersistentDataContainer.nbtRarity: Rarity?
    get() {
        return Rarities[this.get(rarityKey, PersistentDataType.STRING)]
    }
    set(value) {
        if (value != null) {
            this.set(rarityKey, PersistentDataType.STRING, value.id)
        } else {
            this.remove(rarityKey)
        }
    }
