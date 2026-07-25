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

    /** Value-driven frame bars (mana/thirst-style); null for plain text HUDs. */
    val frames = if (config.has("frames")) {
        HudFrames(id, this, config.getSubsection("frames"))
    } else {
        null
    }

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

/**
 * Picks one frame by where a numeric value sits between min and max - the
 * classic image bar (each frame is usually a single :glyph:). The HUD's
 * `text` becomes a template: `%frame%` marks where the frame renders, and an
 * empty text shows the frame alone.
 */
class HudFrames(hudId: String, hud: Hud, config: Config) {
    /** A placeholder/math expression evaluated per player, e.g. "%libreforge_points_mana%". */
    val value: String = config.getString("value")

    val min = config.getDoubleOrNull("min") ?: 0.0
    val max = config.getDoubleOrNull("max") ?: 100.0

    /** Rendered lines, lowest value first. */
    val frames: List<String> = config.getStrings("frames")

    init {
        if (frames.isEmpty()) {
            plugin.logger.warning("HUD $hudId has a frames: section with no frames list")
        }
        if (max <= min) {
            plugin.logger.warning("HUD $hudId frames: max must be greater than min")
        }
        if (hud.text.isNotEmpty() && "%frame%" !in hud.text) {
            plugin.logger.warning("HUD $hudId has frames but its text has no %frame% placeholder; text will be ignored")
        }
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
