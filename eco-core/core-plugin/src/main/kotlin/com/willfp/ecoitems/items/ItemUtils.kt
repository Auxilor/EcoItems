package com.willfp.ecoitems.items

import com.willfp.eco.core.fast.FastItemStack
import com.willfp.eco.core.fast.fast
import com.willfp.eco.util.namespacedKeyOf
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.trim.ArmorTrim
import org.bukkit.persistence.PersistentDataType

private val legacyKey = namespacedKeyOf("ecoweapons", "weapon")
private val key = namespacedKeyOf("ecoitems", "item")

/** The config's display-name at build time, so updates can tell a config default from a player rename. */
val baseDisplayNameKey = namespacedKeyOf("ecoitems", "base-display-name")

/** The config's lore at build time, so updates can tell config lore from lore another plugin added/changed. */
val baseLoreKey = namespacedKeyOf("ecoitems", "base-lore")

/**
 * The config's lore at build time as components, so updates can compare exactly.
 *
 * [baseLoreKey] holds the same lore as legacy strings, which cannot represent
 * everything a component can, so comparing against it reports lore as changed
 * when only its representation differs.
 */
val baseLoreComponentsKey = namespacedKeyOf("ecoitems", "base-lore-components")

/** Separator joining lore lines for [baseLoreKey]; lore lines never contain the null character. */
const val LORE_SEPARATOR = "\u0000"

/** The config's armor trim (material:pattern) at build time, so updates can tell a config trim from an applied one. */
val baseTrimKey = namespacedKeyOf("ecoitems", "base-trim")

fun ArmorTrim.encoded(): String = "${material.key}:${pattern.key}"

fun List<Component>.encoded(): String =
    joinToString(LORE_SEPARATOR) { GsonComponentSerializer.gson().serialize(it) }

var ItemStack?.ecoItem: EcoItem?
    get() {
        this ?: return null
        val fis = this.fast()
        return fis.ecoItem
    }
    set(value) {
        this ?: return
        val fis = this.fast()
        fis.ecoItem = value
    }

var FastItemStack.ecoItem: EcoItem?
    get() {
        val pdc = this.persistentDataContainer

        val legacyId = pdc.get(legacyKey, PersistentDataType.STRING)
        if (legacyId != null) {
            pdc.remove(legacyKey)
            pdc.set(key, PersistentDataType.STRING, legacyId)
        }

        return EcoItems.getByID(pdc.get(key, PersistentDataType.STRING))
    }
    set(value) {
        val pdc = this.persistentDataContainer

        if (value == null) {
            pdc.remove(key)
        } else {
            pdc.set(key, PersistentDataType.STRING, value.id.key)
        }
    }
