package com.willfp.ecoitems.pack

import com.willfp.eco.core.integrations.placeholder.PlaceholderManager
import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.glyphs.Glyphs
import com.willfp.ecoitems.huds.Huds
import com.willfp.ecoitems.items.EcoItems
import com.willfp.ecoitems.nms.ItemComponentsProxy
import com.willfp.ecoitems.blocks.BlockBacking
import com.willfp.ecoitems.blocks.BlockSoundState
import com.willfp.ecoitems.blocks.EcoBlocks
import com.willfp.ecoitems.pack.blocks.BlockSoundsListener
import com.willfp.ecoitems.pack.delivery.PackDelivery
import com.willfp.ecoitems.pack.delivery.PackListener
import com.willfp.ecoitems.pack.glyphs.GlyphCodepoints
import com.willfp.ecoitems.pack.glyphs.GlyphListeners
import com.willfp.ecoitems.pack.glyphs.GlyphPlaceholder
import com.willfp.ecoitems.pack.glyphs.GlyphTabCompletions
import com.willfp.ecoitems.pack.glyphs.GlyphText
import com.willfp.ecoitems.pack.glyphs.ShiftChars
import com.willfp.ecoitems.pack.glyphs.ShiftPlaceholder
import com.willfp.ecoitems.pack.huds.HudState
import com.willfp.ecoitems.pack.huds.HudTicker
import com.willfp.ecoitems.pack.publisher.ExternalPublisher
import com.willfp.ecoitems.pack.publisher.HostedPublisher
import com.willfp.ecoitems.pack.publisher.PackPublisher
import com.willfp.ecoitems.pack.publisher.SelfHostedPublisher
import com.willfp.ecoitems.paintings.Paintings
import com.willfp.ecoitems.sounds.Sounds
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack

object EcoItemsPackFeature : PackFeature {
    private var packYml: PackYml? = null
    private var publisher: PackPublisher? = null

    override fun listeners(plugin: EcoItemsPlugin): List<Listener> {
        return listOf(PackListener, HudTicker.QuitListener, TridentListener, BlockSoundsListener) + GlyphListeners.listeners()
    }

    override fun handleEnable(plugin: EcoItemsPlugin) {
        PlaceholderManager.addIntegration(GlyphText.GlyphPlaceholderIntegration)
        PlaceholderManager.registerPlaceholder(GlyphPlaceholder)
        PlaceholderManager.registerPlaceholder(ShiftPlaceholder)
    }

    override fun handleReload(plugin: EcoItemsPlugin) {
        PackDefaults.ensure(plugin)

        val config = packYml ?: PackYml(plugin).also { packYml = it }
        val settings = PackSettings(config)

        if (!settings.enabled) {
            shutdownPublisher()
            PackDelivery.clear()
            GlyphText.clear()
            HudTicker.stop()
            BlockSoundState.remapActive = false
            return
        }

        HudTicker.start(plugin)

        val imports = PackImports.load(plugin)

        val glyphs = GlyphCodepoints.assign(plugin, Glyphs.values(), imports.fontCodepoints)
        GlyphText.reload(glyphs, settings)
        GlyphTabCompletions.refresh(plugin)

        BlockSoundState.remapActive = settings.customBlockSounds &&
            EcoBlocks.values().any { it.backing == BlockBacking.NOTEBLOCK }

        val assets = EcoItems.values().mapNotNull { ItemPackAsset.fromItem(it) }
        TridentListener.update(assets)
        DatapackGenerator.write(plugin, Paintings.values(), Sounds.values())
        val pack = PackBuilder.build(plugin, settings, assets, glyphs.values, Sounds.values(), Huds.values(), imports)

        val published = resolvePublisher(plugin, settings)?.publish(pack)
        PackDelivery.update(published, settings)

        if (published != null && settings.sendOnReload) {
            PackDelivery.sendAll()
        }
    }

    override fun handleDisable(plugin: EcoItemsPlugin) {
        shutdownPublisher()
        HudTicker.stop()
    }

    override fun toggleHud(player: Player, id: String): Boolean? {
        return HudState.toggle(player, id)
    }

    override fun decorateGuiTitle(plugin: EcoItemsPlugin, title: String, glyphId: String?): String {
        if (glyphId == null) return title

        val assigned = GlyphText.assignments[glyphId]
        if (assigned == null) {
            plugin.logger.warning("GUI background glyph '$glyphId' is not loaded; using the plain title")
            return title
        }

        return ShiftChars.shift(-8) +
            GlyphText.legacyChars(assigned, restore = "§r") +
            ShiftChars.shift(-161) +
            title
    }

    override fun decorateGuiItem(
        plugin: EcoItemsPlugin,
        item: ItemStack,
        model: String?
    ): ItemStack {
        if (model == null) return item

        val result = plugin.getProxy(ItemComponentsProxy::class.java)
            .withComponents(item, mapOf("minecraft:item_model" to model))

        for (error in result.errors) {
            plugin.logger.warning("Invalid GUI item model '$model': $error")
        }

        return result.item
    }

    private fun resolvePublisher(plugin: EcoItemsPlugin, settings: PackSettings): PackPublisher? {
        // The self-hosted server is reused across reloads (unless rebound) so
        // in-flight downloads survive; everything else is stateless.
        val current = publisher
        if (current is SelfHostedPublisher &&
            settings.mode == DeliveryMode.SELF_HOSTED &&
            current.bind == settings.selfHostedBind &&
            current.port == settings.selfHostedPort &&
            current.publicUrl == settings.selfHostedPublicUrl
        ) {
            return current
        }

        shutdownPublisher()

        val created = when (settings.mode) {
            DeliveryMode.HOSTED -> HostedPublisher(plugin, settings.hostedUrl)
            DeliveryMode.SELF_HOSTED -> SelfHostedPublisher(
                plugin,
                settings.selfHostedBind,
                settings.selfHostedPort,
                settings.selfHostedPublicUrl
            )
            DeliveryMode.EXTERNAL -> ExternalPublisher(plugin, settings.externalDirectory, settings.externalUrl)
            DeliveryMode.NONE -> null
        }

        publisher = created
        return created
    }

    private fun shutdownPublisher() {
        publisher?.shutdown()
        publisher = null
    }
}
