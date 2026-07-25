package com.willfp.ecoitems.libreforge

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecoitems.plugin
import com.willfp.libreforge.ViolationContext
import com.willfp.libreforge.effects.Effects
import com.willfp.libreforge.toDispatcher
import com.willfp.libreforge.triggers.TriggerData
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Player

/** Player interactions with placed blocks and furniture. */
enum class ContentEvent(val key: String) {
    PUNCH("punch"),
    SHIFT_PUNCH("shift-punch"),
    RIGHT_CLICK("right-click"),
    SHIFT_RIGHT_CLICK("shift-right-click"),
    PLACE("place"),
    BREAK("break"),
    SIT("sit")
}

/**
 * Effects that run when a player interacts with placed content, configured
 * per event (like EcoMobs' mob events):
 *
 * ```yaml
 * block:
 *   effects:
 *     right-click:
 *       - id: send_message
 *         args:
 *           message: "Hello!"
 * ```
 */
class ContentEffects(owner: String, config: Config) {
    private val chains = ContentEvent.entries.mapNotNull { event ->
        val sections = config.getSubsections("effects.${event.key}")
        if (sections.isEmpty()) {
            return@mapNotNull null
        }

        val chain = Effects.compileChain(
            sections,
            ViolationContext(plugin, "$owner effects ${event.key}")
        ) ?: return@mapNotNull null

        event to chain
    }.toMap()

    fun dispatch(event: ContentEvent, player: Player, location: Location, block: Block? = null) {
        val chain = chains[event] ?: return

        val data = TriggerData(
            player = player,
            location = location,
            block = block,
            item = player.inventory.itemInMainHand
        )

        chain.trigger(data.dispatch(player.toDispatcher()))
    }

    /** The punch/right-click pair for the sneak state. */
    fun clickEvent(rightClick: Boolean, sneaking: Boolean): ContentEvent = when {
        rightClick && sneaking -> ContentEvent.SHIFT_RIGHT_CLICK
        rightClick -> ContentEvent.RIGHT_CLICK
        sneaking -> ContentEvent.SHIFT_PUNCH
        else -> ContentEvent.PUNCH
    }
}
