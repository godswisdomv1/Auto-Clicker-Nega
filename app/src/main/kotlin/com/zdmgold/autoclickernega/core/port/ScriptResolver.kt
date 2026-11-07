package com.zdmgold.autoclickernega.core.port

import com.zdmgold.autoclickernega.core.model.Script
import com.zdmgold.autoclickernega.core.model.TemplateRef
import com.zdmgold.autoclickernega.core.vision.Frame

/** Finds other scripts by id, for Run script actions. */
fun interface ScriptResolver {
    suspend fun resolve(id: String): Script?
}

/** Loads a script's reference images as grayscale frames, at the size they were authored. */
fun interface TemplateProvider {
    suspend fun load(
        scriptId: String,
        ref: TemplateRef,
    ): Frame?
}
