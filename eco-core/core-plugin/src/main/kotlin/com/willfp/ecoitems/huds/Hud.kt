package com.willfp.ecoitems.huds

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.registry.KRegistrable
import com.willfp.ecoitems.plugin
import com.willfp.libreforge.ViolationContext
import com.willfp.libreforge.conditions.Conditions
import net.kyori.adventure.bossbar.BossBar
import java.util.Objects

enum class HudType {
    ACTION_BAR,
    BOSS_BAR;

    companion object {
        fun fromID(id: String): HudType? =
            entries.firstOrNull { it.name.equals(id.replace('-', '_'), ignoreCase = true) }
    }
}

class Hud(
    override val id: String,
    val config: Config
) : KRegistrable {
    val text: String = config.getString("text")

    val type = HudType.fromID(config.getString("type").ifEmpty { "action-bar" })
        ?: HudType.ACTION_BAR.also {
            plugin.logger.warning("HUD $id has unknown type '${config.getString("type")}', using action-bar")
        }

    /** When set, dynamic text renders at this ascent via the ecoitems:hud/<id> font. */
    val textAscent: Int? = config.getIntOrNull("text-ascent")

    val updateTicks = config.getIntOrNull("update-ticks") ?: 40

    val enabledByDefault = config.getBool("enabled-by-default")

    /** Empty = visible to everyone. */
    val permission: String = config.getString("permission")

    val conditions = Conditions.compile(
        config.getSubsections("conditions"),
        ViolationContext(plugin, "HUD ID $id")
    )

    val bossBar = BossBarOptions(id, config.getSubsection("boss-bar"))

    override fun equals(other: Any?): Boolean {
        if (other !is Hud) {
            return false
        }

        return this.id == other.id
    }

    override fun hashCode(): Int {
        return Objects.hash(this.id)
    }

    override fun toString(): String {
        return "Hud{$id}"
    }
}

class BossBarOptions(id: String, config: Config) {
    val color: BossBar.Color = config.getStringOrNull("color")?.let { configured ->
        BossBar.Color.entries.firstOrNull { it.name.equals(configured, ignoreCase = true) }
            ?: BossBar.Color.WHITE.also {
                plugin.logger.warning("HUD $id has unknown boss-bar color '$configured', using white")
            }
    } ?: BossBar.Color.WHITE

    val overlay: BossBar.Overlay = config.getStringOrNull("style")?.let { configured ->
        BossBar.Overlay.entries.firstOrNull { it.name.equals(configured, ignoreCase = true) }
            ?: BossBar.Overlay.PROGRESS.also {
                plugin.logger.warning("HUD $id has unknown boss-bar style '$configured', using progress")
            }
    } ?: BossBar.Overlay.PROGRESS

    val progress: Float = (config.getDoubleOrNull("progress") ?: 1.0).toFloat().coerceIn(0f, 1f)
}
