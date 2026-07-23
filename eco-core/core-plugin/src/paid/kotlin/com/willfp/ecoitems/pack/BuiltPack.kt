package com.willfp.ecoitems.pack

import java.io.File

class BuiltPack(
    val file: File,
    val sha1: String
) {
    val sha1Bytes: ByteArray = sha1.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
