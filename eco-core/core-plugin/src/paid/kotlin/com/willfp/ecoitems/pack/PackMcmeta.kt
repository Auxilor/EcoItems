package com.willfp.ecoitems.pack

object PackMcmeta {
    // Resource pack formats: 64 = 1.21.8, 88 = 26.2. Bump MAX_FORMAT on new
    // Minecraft releases. Server-sent packs apply even on a format mismatch,
    // so a stale value is cosmetic.
    const val MIN_FORMAT = 64
    const val MAX_FORMAT = 88

    // Clients on 1.21.8 read pack_format/supported_formats and ignore the
    // 1.21.9+ min_format/max_format keys; newer clients do the opposite.
    // Overlay entries need both key styles: 1.21.8 reads "formats" and
    // ignores min_format/max_format, newer clients do the opposite. The two
    // must agree, and one entry has to start at format 64, or newer clients
    // reject "formats" as deprecated.
    //
    // The ranges are disjoint: a client must only ever see shaders written
    // for it. Anything it can't resolve - a #moj_import of an include that
    // doesn't exist yet, GLSL 330 before 1.21.9 - fails the whole pack, even
    // in files nothing renders with.
    //
    // 64 = 1.21.8 (GLSL 150), 65-83 = 1.21.9 through 1.21.11 (GLSL 330),
    // 84-87 = 26.1 (adds sample_lightmap.glsl), 88+ = 26.2 (text shaders
    // merged into one program with defines).
    private val SHADER_OVERLAY_ENTRIES = listOf(
        """{ "formats": { "min_inclusive": 88, "max_inclusive": 999 }, "min_format": 88, "max_format": 999, "directory": "overlay_26_2" }""",
        """{ "formats": { "min_inclusive": 84, "max_inclusive": 87 }, "min_format": 84, "max_format": 87, "directory": "overlay_26" }""",
        """{ "formats": { "min_inclusive": 65, "max_inclusive": 83 }, "min_format": 65, "max_format": 83, "directory": "overlay_pre_26" }""",
        """{ "formats": 64, "min_format": 64, "max_format": 64, "directory": "overlay_1_21_8" }"""
    )

    /**
     * Overlay directories for assets - not shaders - that only exist on newer
     * clients: content in them is added on top of the base pack rather than
     * replacing it, so nothing has to be dropped to keep older clients
     * loading. Declared only when the built pack actually has the directory.
     */
    val CONTENT_OVERLAYS = mapOf(
        "overlay_26_plus" to
            """{ "formats": { "min_inclusive": 84, "max_inclusive": 999 }, "min_format": 84, "max_format": 999, "directory": "overlay_26_plus" }"""
    )

    fun json(
        description: String,
        withShaderOverlays: Boolean = false,
        importedOverlays: List<String> = emptyList(),
        contentOverlays: List<String> = emptyList()
    ): String {
        // Later entries win, so imported overlays go first: our overlays must
        // not be shadowed by imported packs.
        val entries = importedOverlays + contentOverlays +
            if (withShaderOverlays) SHADER_OVERLAY_ENTRIES else emptyList()

        val overlays = if (entries.isEmpty()) "" else """,
  "overlays": {
    "entries": [
      ${entries.joinToString(",\n      ")}
    ]
  }"""

        return """
{
  "pack": {
    "description": ${description.toJsonString()},
    "pack_format": $MIN_FORMAT,
    "supported_formats": {
      "min_inclusive": $MIN_FORMAT,
      "max_inclusive": $MAX_FORMAT
    },
    "min_format": $MIN_FORMAT,
    "max_format": $MAX_FORMAT
  }$overlays
}
""".trimStart()
    }
}

internal fun String.toJsonString(): String =
    "\"" + this
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n") + "\""
