package com.zdmgold.autoclickernega.service.capture

import android.graphics.BitmapFactory
import com.zdmgold.autoclickernega.core.model.TemplateRef
import com.zdmgold.autoclickernega.core.port.TemplateProvider
import com.zdmgold.autoclickernega.core.port.TemplateStore
import com.zdmgold.autoclickernega.core.result.Outcome
import com.zdmgold.autoclickernega.core.vision.Frame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads a script's reference crops from the template store and decodes them to grayscale frames. */
class StoreTemplateProvider(
    private val store: TemplateStore,
) : TemplateProvider {
    override suspend fun load(
        scriptId: String,
        ref: TemplateRef,
    ): Frame? {
        val bytes = (store.load(scriptId, ref.id) as? Outcome.Success)?.value ?: return null
        return withContext(Dispatchers.Default) {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext null
            try {
                BitmapToFrame.convert(bitmap, keepColor = false)
            } finally {
                bitmap.recycle()
            }
        }
    }
}
