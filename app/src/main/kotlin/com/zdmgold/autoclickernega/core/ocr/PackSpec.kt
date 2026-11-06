package com.zdmgold.autoclickernega.core.ocr

/** One language pack the app knows how to receive: its language, and the SHA-256 the downloaded files must have. */
data class PackSpec(
    val lang: String,
    val sha256: String,
)
