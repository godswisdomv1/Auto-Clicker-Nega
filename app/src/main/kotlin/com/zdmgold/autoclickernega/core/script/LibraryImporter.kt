package com.zdmgold.autoclickernega.core.script

import com.zdmgold.autoclickernega.core.model.Script
import com.zdmgold.autoclickernega.core.port.ScriptStore
import com.zdmgold.autoclickernega.core.port.TemplateStore
import com.zdmgold.autoclickernega.core.result.Outcome

/**
 * Adds a picked or pasted file to the library. The bytes are checked by [ScriptImporter], the script gets a new id
 * so it can never overwrite one the user already has, and its images are stored beside it. If an image cannot be
 * stored the script is taken back out again, so the library never holds a script with missing pictures.
 */
object LibraryImporter {
    suspend fun importInto(
        bytes: ByteArray,
        scripts: ScriptStore,
        templates: TemplateStore,
    ): Outcome<Script> {
        val bundle =
            when (val imported = ScriptImporter.import(bytes)) {
                is Outcome.Failure -> return imported
                is Outcome.Success -> imported.value
            }
        val stored =
            when (val saved = scripts.save(bundle.script.copy(id = ""))) {
                is Outcome.Failure -> return saved
                is Outcome.Success -> saved.value
            }
        for ((templateId, png) in bundle.templates) {
            val result = templates.save(stored.id, templateId, png)
            if (result is Outcome.Failure) {
                scripts.delete(stored.id)
                templates.deleteAll(stored.id)
                return result
            }
        }
        return Outcome.Success(stored)
    }
}
