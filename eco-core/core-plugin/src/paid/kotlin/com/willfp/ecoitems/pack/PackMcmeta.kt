package com.willfp.ecoitems.pack

object PackMcmeta {
    // Resource pack formats: 64 = 1.21.8, 88 = 26.2. Bump MAX_FORMAT on new
    // Minecraft releases. Server-sent packs apply even on a format mismatch,
    // so a stale value is cosmetic.
    const val MIN_FORMAT = 64
    const val MAX_FORMAT = 88

    // Clients on 1.21.8 read pack_format/supported_formats and ignore the
    // 1.21.9+ min_format/max_format keys; newer clients do the opposite.
    // The shader overlays swap in newer GLSL for newer clients; the base
    // pack carries the 1.21.8-era shaders.
    // Overlay entries need both key styles: 1.21.8 requires "formats", while
    // newer clients require min_format/max_format. Newer clients reject the
    // legacy key on overlays beginning after format 64, so the overlays span
    // back to 64 and are ordered newest-first. Later overlays restore the
    // correct shaders for older formats.
    private const val SHADER_OVERLAYS = """,
  "overlays": {
    "entries": [
      { "formats": 64, "min_format": 64, "max_format": 999, "directory": "overlay_26_2" },
      { "formats": 64, "min_format": 64, "max_format": 87, "directory": "overlay_26" },
      { "formats": 64, "min_format": 64, "max_format": 83, "directory": "overlay_pre_26" }
    ]
  }"""

    fun json(description: String, withShaderOverlays: Boolean = false): String =
        """
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
  }${if (withShaderOverlays) SHADER_OVERLAYS else ""}
}
""".trimStart()
}

internal fun String.toJsonString(): String =
    "\"" + this
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n") + "\""
