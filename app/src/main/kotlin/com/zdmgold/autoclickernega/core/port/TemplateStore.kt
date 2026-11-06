package com.zdmgold.autoclickernega.core.port

import com.zdmgold.autoclickernega.core.result.Outcome

/** Reference crops, as PNG bytes, kept per script. Pixels never live inside a script. */
interface TemplateStore {
    suspend fun save(
        scriptId: String,
        templateId: String,
        png: ByteArray,
    ): Outcome<Unit>

    /** The image, or null when it does not exist. */
    suspend fun load(
        scriptId: String,
        templateId: String,
    ): Outcome<ByteArray?>

    suspend fun delete(
        scriptId: String,
        templateId: String,
    ): Outcome<Unit>

    suspend fun deleteAll(scriptId: String): Outcome<Unit>

    suspend fun list(scriptId: String): Outcome<List<String>>
}
