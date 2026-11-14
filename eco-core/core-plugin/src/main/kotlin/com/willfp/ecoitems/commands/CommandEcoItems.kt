package com.willfp.ecoitems.commands

import com.willfp.ecoitems.EcoItemsPlugin
import wiki.ecohub.core.command.command

internal fun EcoItemsPlugin.ecoItemsCommand() = command(name = "ecoitems", permission = "ecoitems.command.ecoitems") {
    aliases("ei", "e", "ecoi", "items")

    reloadCommand()
    giveCommand()
    guiCommand()
    hudCommand()
    glyphCommand()
    glyphsCommand()
    totemCommand()
    dropCommand()
    takeCommand()
    renameCommand()
    repairCommand()
    durabilityCommand()
    hitboxCommand()
    dialogCommand()
    migrateCommand()
}
