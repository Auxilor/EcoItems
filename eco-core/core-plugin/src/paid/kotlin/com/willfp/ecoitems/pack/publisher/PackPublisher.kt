package com.willfp.ecoitems.pack.publisher

import com.willfp.ecoitems.pack.BuiltPack

class PublishedPack(
    val url: String,
    val sha1: String,
    val sha1Bytes: ByteArray
) {
    constructor(url: String, pack: BuiltPack) : this(url, pack.sha1, pack.sha1Bytes)
}

interface PackPublisher {
    /**
     * Makes the pack available for download, returning where; or null if
     * publishing failed (delivery is disabled until the next reload).
     */
    fun publish(pack: BuiltPack): PublishedPack?

    fun shutdown() {}
}
