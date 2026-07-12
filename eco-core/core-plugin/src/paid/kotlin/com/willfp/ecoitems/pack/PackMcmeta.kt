package com.willfp.ecoitems.pack

object PackMcmeta {
    // Resource pack formats: 64 = 1.21.8, 88 = 26.2. Bump MAX_FORMAT on new
    // Minecraft releases. Server-sent packs apply even on a format mismatch,
    // so a stale value is cosmetic.
    const val MIN_FORMAT = 64
    const val MAX_FORMAT = 88

    // Clients on 1.21.8 read pack_format/supported_formats and ignore the
    // 1.21.9+ min_format/max_format keys; newer clients do the opposite.
    fun json(description: String): String = """
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
          }
        }
    """.trimIndent()
}

internal fun String.toJsonString(): String =
    "\"" + this
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n") + "\""
