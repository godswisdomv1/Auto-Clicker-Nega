package com.zdmgold.autoclickernega.core.model

/** Metadata of one reference crop. The pixels live in the template store, never in the script. */
data class TemplateRef(
    val id: String,
    val file: String,
    val w: Int,
    val h: Int,
    val refDensity: Int,
)
