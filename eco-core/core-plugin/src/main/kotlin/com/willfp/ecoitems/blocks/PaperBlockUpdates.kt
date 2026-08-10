package com.willfp.ecoitems.blocks

import java.io.File

/**
 * Paper can skip the block updates that normalize our hijacked blockstates.
 * The listeners cannot cover every path - since 1.20 the note block
 * instrument is recalculated by a shape update, which is never wrapped in
 * BlockPhysicsEvent - so the flags are the only complete fix.
 *
 * EcoItems never edits paper-global.yml itself - Paper's TOS prohibits
 * plugins from modifying server config - so this only reads the file to
 * decide whether to warn.
 */
object PaperBlockUpdates {
    const val NOTEBLOCK_FLAG = "disable-noteblock-updates"
    const val TRIPWIRE_FLAG = "disable-tripwire-updates"
    const val CHORUS_FLAG = "disable-chorus-plant-updates"
    const val MUSHROOM_FLAG = "disable-mushroom-block-updates"

    private val file = File("config/paper-global.yml")

    private val settingPattern = Regex(
        "^(\\s*)($NOTEBLOCK_FLAG|$TRIPWIRE_FLAG|$CHORUS_FLAG|$MUSHROOM_FLAG):\\s*(true|false)\\b.*$",
        RegexOption.IGNORE_CASE
    )

    /** Whether a flag is currently set to true in paper-global.yml. */
    fun isEnabled(flag: String): Boolean =
        parse()[flag] == true

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
        forEachSettingLine(lines) { _, name, value ->
            settings[name] = value == "true"
        }
        return settings
    }

    private inline fun forEachSettingLine(
        lines: List<String>,
        action: (indent: String, name: String, value: String) -> Unit
    ) {
        var inSection = false
        var sectionIndent = -1

        for (line in lines) {
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
            action(match.groupValues[1], match.groupValues[2].lowercase(), match.groupValues[3].lowercase())
        }
    }
}
