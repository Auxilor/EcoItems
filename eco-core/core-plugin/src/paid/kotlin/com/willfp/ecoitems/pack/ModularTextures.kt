package com.willfp.ecoitems.pack

import com.willfp.ecoitems.items.EcoItem
import com.willfp.ecoitems.materials.ModularConfig
import com.willfp.ecoitems.materials.ModularItemService

internal fun EcoItem.partTexture(state: String? = null): PackLocation? {
    val part = part ?: return null
    return (state?.let { part.stateTextures[it] } ?: part.texture)?.let { PackLocation.parse(it) }
}

/** The part's texture and every animation state texture, all recoloured per material. */
internal fun EcoItem.allPartTextures(): List<PackLocation> =
    part?.run { listOfNotNull(texture) + stateTextures.values }.orEmpty().mapNotNull { PackLocation.parse(it) }

/** The textures a modular item draws in place of its parts' own. */
internal fun EcoItem.slotTextures(): List<PackLocation> =
    modular?.run { slotTextures.values }.orEmpty().mapNotNull { PackLocation.parse(it) }

/** The texture drawn for [slotId]: the item's own slot texture when idle, otherwise the part's. */
internal fun ModularConfig.layerTexture(slotId: String, state: String?): PackLocation? =
    slotTextures[slotId]?.takeIf { state == null }?.let { PackLocation.parse(it) }
        ?: slots[slotId]?.let { ModularItemService.partItemForShape(it)?.partTexture(state) }

internal fun EcoItem.overlayTextures(state: String? = null): List<PackLocation> {
    val modular = modular ?: return emptyList()
    return (modular.overlays + state?.let { modular.stateOverlays[it] }.orEmpty()).mapNotNull { PackLocation.parse(it) }
}

/** Every overlay the item can draw, in any animation state. */
internal fun EcoItem.allOverlayTextures(): List<PackLocation> =
    modular?.run { overlays + stateOverlays.values.flatten() }.orEmpty().mapNotNull { PackLocation.parse(it) }
