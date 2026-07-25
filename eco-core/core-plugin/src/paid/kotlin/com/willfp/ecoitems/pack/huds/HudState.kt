package com.willfp.ecoitems.pack.huds

import com.willfp.eco.core.data.keys.PersistentDataKey
import com.willfp.eco.core.data.keys.PersistentDataKeyType
import com.willfp.eco.core.data.profile
import com.willfp.eco.util.namespacedKeyOf
import com.willfp.ecoitems.huds.Hud
import com.willfp.ecoitems.huds.HudType
import com.willfp.ecoitems.huds.Huds
import org.bukkit.entity.Player

/**
 * Per-player HUD choices, persisted through eco profiles.
 *
 * Action bar HUDs occupy a single slot per player; boss bar HUDs stack, each
 * independently toggleable against its enabled-by-default value.
 */
object HudState {
    private const val DEFAULT = "_default"
    private const val NONE = "_none"

    private val actionBarKey = PersistentDataKey(
        namespacedKeyOf("ecoitems", "hud_actionbar"),
        PersistentDataKeyType.STRING,
        DEFAULT
    )

    private val enabledKey = PersistentDataKey(
        namespacedKeyOf("ecoitems", "hud_enabled"),
        PersistentDataKeyType.STRING_LIST,
        emptyList()
    )

    private val disabledKey = PersistentDataKey(
        namespacedKeyOf("ecoitems", "hud_disabled"),
        PersistentDataKeyType.STRING_LIST,
        emptyList()
    )

    fun activeActionBarHud(player: Player): Hud? {
        return when (val id = player.profile.read(actionBarKey)) {
            NONE -> null
            DEFAULT -> defaultActionBarHud()
            else -> Huds.getByID(id)?.takeIf { it.type == HudType.ACTION_BAR }
                ?: defaultActionBarHud() // The chosen HUD no longer exists.
        }
    }

    fun isBossBarVisible(player: Player, hud: Hud): Boolean {
        if (hud.id in player.profile.read(enabledKey)) {
            return true
        }
        if (hud.id in player.profile.read(disabledKey)) {
            return false
        }
        return hud.enabledByDefault
    }

    /** Returns the HUD's new visibility, or null if the id isn't a HUD. */
    fun toggle(player: Player, id: String): Boolean? {
        val hud = Huds.getByID(id) ?: return null

        return when (hud.type) {
            HudType.ACTION_BAR -> {
                val showing = activeActionBarHud(player)?.id == hud.id
                player.profile.write(actionBarKey, if (showing) NONE else hud.id)
                !showing
            }

            HudType.BOSS_BAR -> {
                val visible = isBossBarVisible(player, hud)

                val enabled = player.profile.read(enabledKey).toMutableList().apply { remove(hud.id) }
                val disabled = player.profile.read(disabledKey).toMutableList().apply { remove(hud.id) }
                if (visible) disabled.add(hud.id) else enabled.add(hud.id)

                player.profile.write(enabledKey, enabled)
                player.profile.write(disabledKey, disabled)
                !visible
            }
        }
    }

    private fun defaultActionBarHud(): Hud? =
        Huds.values().firstOrNull { it.type == HudType.ACTION_BAR && it.enabledByDefault }
}
