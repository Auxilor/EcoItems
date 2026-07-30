package com.willfp.ecoitems.blocks

import com.willfp.ecoitems.EcoItemsPlugin
import java.io.File

/**
 * Paper can skip the block updates that normalize our hijacked blockstates.
 * The listeners cannot cover every path - since 1.20 the note block
 * instrument is recalculated by a shape update, which is never wrapped in
 * BlockPhysicsEvent - so the flags are the only complete fix.
 *
 * The file is edited line by line rather than round-tripped through a YAML
 * parser: paper-global.yml is not ours, and a parser would strip every
 * comment in it.
 */
object PaperBlockUpdates {
    const val NOTEBLOCK_FLAG = "disable-noteblock-updates"
    const val TRIPWIRE_FLAG = "disable-tripwire-updates"
    const val CHORUS_FLAG = "disable-chorus-plant-updates"

    private val file = File("config/paper-global.yml")

    private val settingPattern = Regex(
        "^(\\s*)($NOTEBLOCK_FLAG|$TRIPWIRE_FLAG|$CHORUS_FLAG):\\s*(true|false)\\b.*$",
        RegexOption.IGNORE_CASE
    )

    /** Whether a flag is currently set to true in paper-global.yml. */
    fun isEnabled(flag: String): Boolean =
        parse()[flag] == true

    /**
     * Flips each named flag from false to true, returning the names actually
     * changed. Only rewrites keys that already exist and are explicitly
     * false - a missing key or a missing file is left alone.
     */
    fun ensureDisabled(plugin: EcoItemsPlugin, flags: Collection<String>): List<String> {
        if (!plugin.configYml.getBool("blocks.auto-update-paper-config")) {
            return emptyList()
        }

        if (System.getProperty("ecoitems.autoUpdatePaperConfig") == "false") {
            return emptyList()
        }

        if (!file.exists()) {
            return emptyList()
        }

        val text = try {
            file.readText()
        } catch (e: Exception) {
            plugin.logger.warning("Failed to read config/paper-global.yml: ${e.message}")
            return emptyList()
        }

        // Split and rejoin on the file's own separator so a changed flag is a
        // one-line diff rather than a whole-file line ending rewrite.
        val separator = if (text.contains("\r\n")) "\r\n" else "\n"
        val lines = text.split("\r\n", "\n").toMutableList()

        val changed = mutableListOf<String>()

        forEachSettingLine(lines) { index, indent, name, value ->
            if (name in flags && value == "false") {
                lines[index] = "$indent$name: true"
                changed += name
            }
        }

        if (changed.isEmpty()) {
            return emptyList()
        }

        return try {
            file.writeText(lines.joinToString(separator))
            changed
        } catch (e: Exception) {
            plugin.logger.warning("Failed to write config/paper-global.yml: ${e.message}")
            emptyList()
        }
    }

    private fun parse(): Map<String, Boolean> {
        if (!file.exists()) {
            return emptyMap()
        }

        val lines = try {
            file.readLines()
        } catch (e: Exception) {
            return emptyMap()
        }

        val settings = mutableMapOf<String, Boolean>()
        forEachSettingLine(lines) { _, _, name, value ->
            settings[name] = value == "true"
        }
        return settings
    }

    private inline fun forEachSettingLine(
        lines: List<String>,
        action: (index: Int, indent: String, name: String, value: String) -> Unit
    ) {
        var inSection = false
        var sectionIndent = -1

        for ((index, line) in lines.withIndex()) {
            val trimmed = line.trim()

            if (trimmed.startsWith("block-updates:")) {
                inSection = true
                sectionIndent = line.indexOfFirst { !it.isWhitespace() }
                continue
            }

            if (!inSection || trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue
            }

            val indent = line.indexOfFirst { !it.isWhitespace() }
            if (indent <= sectionIndent) {
                inSection = false
                continue
            }

            val match = settingPattern.matchEntire(line) ?: continue
            action(index, match.groupValues[1], match.groupValues[2].lowercase(), match.groupValues[3].lowercase())
        }
    }
}
