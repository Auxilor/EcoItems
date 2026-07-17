package com.willfp.ecoitems

import com.willfp.eco.core.bstats.EcoMetricsChart
import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.eco.core.display.DisplayModule
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.packet.PacketListener
import com.willfp.eco.core.blocks.Blocks
import com.willfp.ecoitems.blocks.BlockBreakSpeed
import com.willfp.ecoitems.blocks.BlockListener
import com.willfp.ecoitems.blocks.BlockPhysicsListener
import com.willfp.ecoitems.blocks.EcoBlocks
import com.willfp.ecoitems.blocks.PaperBlockListener
import com.willfp.ecoitems.furniture.FurnitureListener
import com.willfp.ecoitems.furniture.FurnitureStorageManager
import com.willfp.ecoitems.commands.CommandEcoItems
import com.willfp.ecoitems.display.ItemsDisplay
import com.willfp.ecoitems.display.RarityDisplay
import com.willfp.ecoitems.glyphs.Glyphs
import com.willfp.ecoitems.huds.Huds
import com.willfp.ecoitems.nms.ActionBarDetectionProxy
import com.willfp.ecoitems.items.EcoItemFinder
import com.willfp.ecoitems.items.EcoItems
import com.willfp.ecoitems.items.EcoItemsRecipes
import com.willfp.ecoitems.items.ItemListener
import com.willfp.ecoitems.items.ItemUpdater
import com.willfp.ecoitems.items.ItemsGUI
import com.willfp.ecoitems.libreforge.ConditionHasEcoItem
import com.willfp.ecoitems.loots.LootListener
import com.willfp.ecoitems.loots.Loots
import com.willfp.ecoitems.migration.Migrations
import com.willfp.ecoitems.pack.PackFeatures
import com.willfp.ecoitems.paintings.PaintingListener
import com.willfp.ecoitems.paintings.Paintings
import com.willfp.ecoitems.rarity.ArgParserRarity
import com.willfp.ecoitems.rarity.Rarities
import com.willfp.ecoitems.sounds.Sounds
import com.willfp.ecoitems.items.EcoItemTag
import com.willfp.ecoitems.util.DiscoverRecipeListener
import com.willfp.ecoitems.util.PickBlockListener
import com.willfp.libreforge.conditions.Conditions
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.ConfigCategory
import com.willfp.libreforge.registerHolderProvider
import org.bukkit.event.Listener
import java.io.File

internal lateinit var plugin: EcoItemsPlugin
    private set

class EcoItemsPlugin : LibreforgePlugin() {
    init {
        plugin = this
    }

    /** The plugin jar, for reading bundled resources. */
    internal val jar: File
        get() = file

    override fun handleEnable() {
        Items.registerArgParser(ArgParserRarity)
        Items.registerTag(EcoItemTag)

        Conditions.register(ConditionHasEcoItem)

        registerHolderProvider(EcoItemFinder.toHolderProvider())

        Blocks.registerBlockProvider(EcoBlocks.Provider)

        Migrations.ensureFolders(this)

        PackFeatures.instance?.handleEnable(this)
    }

    override fun handleReload() {
        FurnitureStorageManager.persistAll()
        EcoBlocks.reload(this)
        PackFeatures.instance?.handleReload(this)
        ItemsGUI.reload()
        ItemUpdater.updateOnlinePlayers()
    }

    override fun loadPacketListeners(): List<PacketListener> {
        return listOf(
            getProxy(ActionBarDetectionProxy::class.java)
        )
    }

    override fun handleDisable() {
        FurnitureStorageManager.persistAll()
        PackFeatures.instance?.handleDisable(this)
    }

    override fun loadConfigCategories(): List<ConfigCategory> {
        // Sounds and paintings load before items so component warnings can
        // recognise references to entries pending datapack registration.
        return listOf(
            Rarities,
            Sounds,
            Paintings,
            EcoItems,
            EcoItemsRecipes,
            Glyphs,
            Huds,
            Loots
        )
    }

    override fun loadListeners(): List<Listener> {
        return listOf(
            DiscoverRecipeListener,
            ItemListener,
            ItemUpdater,
            PaintingListener,
            BlockListener,
            BlockPhysicsListener,
            BlockBreakSpeed,
            FurnitureListener,
            FurnitureStorageManager,
            PickBlockListener,
            LootListener
        ) + listOfNotNull(PaperBlockListener.createIfSupported()) +
            (PackFeatures.instance?.listeners(this) ?: emptyList())
    }

    override fun loadPluginCommands(): List<PluginCommand> {
        return listOf(
            CommandEcoItems
        )
    }

    override fun loadDisplayModules(): List<DisplayModule> {
        return listOf(
            ItemsDisplay,
            RarityDisplay
        )
    }

    override fun getCustomCharts() = listOf(
        EcoMetricsChart.SingleLine("total_items") { EcoItems.values().size },
        EcoMetricsChart.SingleLine("total_rarities") { Rarities.values().size },
        EcoMetricsChart.SingleLine("total_recipes") { EcoItemsRecipes.size },
        EcoMetricsChart.SimplePie("rarity_enabled") {
            if (configYml.getBool("rarity.enabled")) "enabled" else "disabled"
        },
        EcoMetricsChart.SimplePie("discover_recipes") {
            if (configYml.getBool("discover-recipes")) "enabled" else "disabled"
        }
    )
}
