package com.willfp.ecoitems.premium

import com.willfp.ecoitems.BuildConfig
import com.willfp.ecoitems.plugin
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.util.concurrent.ConcurrentHashMap

/** A feature that only works on the paid version of EcoItems. */
enum class PremiumFeature(val displayName: String) {
    ITEM_LIMIT("Items over the free limit of ${PremiumFeatures.FREE_LIMIT}"),
    RECIPE_LIMIT("Recipes over the free limit of ${PremiumFeatures.FREE_LIMIT}"),
    TEXTURES("Item textures and models"),
    FURNITURE_STATES("Furniture state models"),
    SOUNDS("Custom sounds"),
    PAINTINGS("Custom paintings"),
    GLYPHS("Glyphs"),
    HUDS("HUDs"),
    MATERIALS("Materials"),
    PARTS("Parts"),
    MODULAR_ITEMS("Modular items"),
    KEEP_WHEN_BROKEN("Keep when broken"),
    RAISE_MINING_TIER("The raise_mining_tier effect")
}

/**
 * Collects the premium features a free server has configured during a reload, then reports them
 * once: a grouped list in the console when it changes, and a short notice to admins in game.
 */
object PremiumFeatures : Listener {
    const val FREE_LIMIT = 10

    private const val NOTICE_PERMISSION = "ecoitems.premiumnotice"

    private const val SHOWN_IDS = 10

    private val pending = ConcurrentHashMap<PremiumFeature, MutableSet<String>>()

    @Volatile
    private var configured: Map<PremiumFeature, Set<String>> = emptyMap()

    /** On the free version, records that [id] uses [feature] so the next [publish] lists it. */
    fun report(feature: PremiumFeature, id: String) {
        if (BuildConfig.FREE_VERSION) {
            pending.computeIfAbsent(feature) { ConcurrentHashMap.newKeySet() }.add(id)
        }
    }

    /** True on the free version, after reporting that [id] uses [feature]. */
    fun isLocked(feature: PremiumFeature, id: String): Boolean {
        report(feature, id)
        return BuildConfig.FREE_VERSION
    }

    /** Ends a reload: logs what was recorded since the last one, unless it is the same list as before. */
    fun publish() {
        val previous = configured
        configured = pending.toSortedMap().mapValues { (_, ids) -> ids.sorted().toSet() }
        pending.clear()

        if (configured != previous) {
            logList()
        }
    }

    /** Tells [sender] which premium features are configured but turned off, if any. */
    fun notify(sender: CommandSender) {
        if (configured.isEmpty() || !sender.hasPermission(NOTICE_PERMISSION)) {
            return
        }
        if (sender !is Player) {
            logList()
            return
        }
        sender.sendMessage(
            plugin.langYml.getMessage("premium-features-disabled")
                .replace(oldValue = "%features%", newValue = configured.keys.joinToString { it.displayName })
        )
    }

    private fun logList() {
        if (configured.isEmpty()) {
            return
        }
        plugin.logger.warning("These configured features need the paid version of EcoItems and are turned off:")
        for ((feature, ids) in configured) {
            val shown = ids.take(SHOWN_IDS).joinToString()
            val more = if (ids.size > SHOWN_IDS) " and ${ids.size - SHOWN_IDS} more" else ""
            plugin.logger.warning("- ${feature.displayName} (${ids.size}): $shown$more")
        }
        plugin.logger.warning("Purchase the full version of EcoItems to use them!")
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        notify(event.player)
    }
}
