package com.willfp.ecoitems.furniture

import com.willfp.eco.util.formatEco
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.UUID

/**
 * Chest-style furniture inventories. Contents persist in the base entity's
 * PDC; while open, everyone shares one live inventory per placement (or per
 * player for personal storage), so concurrent viewers see each other's edits.
 */
object FurnitureStorageManager : Listener {
    private val open = mutableMapOf<String, Inventory>()

    private class StorageHolder(
        val baseId: UUID,
        val sessionKey: String,
        val dataKey: NamespacedKey?,
        val storage: FurnitureStorage
    ) : InventoryHolder {
        lateinit var backing: Inventory

        override fun getInventory(): Inventory = backing
    }

    fun openStorage(placed: PlacedFurniture, storage: FurnitureStorage, player: Player) {
        val baseId = placed.base.uniqueId

        val (sessionKey, dataKey) = when (storage.type) {
            StorageType.STORAGE -> "$baseId" to sharedKey
            StorageType.PERSONAL -> "$baseId/${player.uniqueId}" to personalKey(player)
            StorageType.DISPOSAL -> "$baseId/${player.uniqueId}/disposal" to null
        }

        val inventory = open.getOrPut(sessionKey) {
            val holder = StorageHolder(baseId, sessionKey, dataKey, storage)
            val created = Bukkit.createInventory(holder, storage.rows * 9, storage.title.formatEco())
            holder.backing = created

            if (dataKey != null) {
                val stored = placed.base.persistentDataContainer.get(dataKey, PersistentDataType.BYTE_ARRAY)
                if (stored != null) {
                    deserialize(stored).forEachIndexed { slot, stack ->
                        if (slot < created.size) created.setItem(slot, stack)
                    }
                }
            }

            created
        }

        player.openInventory(inventory)
        player.world.playSound(placed.base.location, storage.openSound, SoundCategory.BLOCKS, 1.0f, 1.0f)
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        val holder = event.inventory.holder as? StorageHolder ?: return

        persist(holder)

        // The closer is still counted as a viewer during this event.
        if (event.inventory.viewers.size <= 1) {
            open.remove(holder.sessionKey)
        }

        val base = Bukkit.getEntity(holder.baseId)
        if (base != null) {
            event.player.world.playSound(
                base.location, holder.storage.closeSound, SoundCategory.BLOCKS, 1.0f, 1.0f
            )
        }
    }

    /** Persists every open inventory (reloads and shutdowns). */
    fun persistAll() {
        for (inventory in open.values.toList()) {
            (inventory.holder as? StorageHolder)?.let { persist(it) }
            inventory.viewers.toList().forEach { it.closeInventory() }
        }
        open.clear()
    }

    /**
     * The stored shared contents for a broken placement, from the live
     * session when one is open. Personal/disposal contents are not dropped.
     */
    fun takeContents(placed: PlacedFurniture): List<ItemStack> {
        val baseId = placed.base.uniqueId

        // Close every session belonging to this placement.
        val sessions = open.filterKeys { it.startsWith("$baseId") }
        for ((key, inventory) in sessions) {
            (inventory.holder as? StorageHolder)?.let { persist(it) }
            inventory.viewers.toList().forEach { it.closeInventory() }
            open.remove(key)
        }

        val stored = placed.base.persistentDataContainer.get(sharedKey, PersistentDataType.BYTE_ARRAY)
            ?: return emptyList()

        return deserialize(stored).filterNotNull().filter { !it.type.isAir }
    }

    private fun persist(holder: StorageHolder) {
        val dataKey = holder.dataKey ?: return // Disposal discards.
        val base = Bukkit.getEntity(holder.baseId) ?: return

        base.persistentDataContainer.set(
            dataKey,
            PersistentDataType.BYTE_ARRAY,
            serialize(holder.backing.contents)
        )
    }

    private val sharedKey = NamespacedKey(com.willfp.ecoitems.plugin, "furniture-storage")

    private fun personalKey(player: Player) =
        NamespacedKey(com.willfp.ecoitems.plugin, "furniture-storage-${player.uniqueId}")

    private fun serialize(contents: Array<ItemStack?>): ByteArray =
        ByteArrayOutputStream().use { bytes ->
            BukkitObjectOutputStream(bytes).use { out ->
                out.writeInt(contents.size)
                contents.forEach { out.writeObject(it) }
            }
            bytes.toByteArray()
        }

    private fun deserialize(data: ByteArray): List<ItemStack?> =
        runCatching {
            BukkitObjectInputStream(ByteArrayInputStream(data)).use { input ->
                List(input.readInt()) { input.readObject() as? ItemStack }
            }
        }.getOrElse { emptyList() }
}
