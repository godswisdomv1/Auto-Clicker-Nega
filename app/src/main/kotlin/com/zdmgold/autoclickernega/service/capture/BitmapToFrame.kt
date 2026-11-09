package com.zdmgold.autoclickernega.service.capture

import android.graphics.Bitmap
import com.zdmgold.autoclickernega.core.vision.Frame

/** Reads a software bitmap into a [Frame] a band of rows at a time, so no second full-size pixel copy is needed. */
object BitmapToFrame {
    private const val ROWS_PER_BAND = 64

    fun convert(
        bitmap: Bitmap,
        keepColor: Boolean,
    ): Frame {
        val w = bitmap.width
        return Frame.fromRows(w, bitmap.height, keepColor, ROWS_PER_BAND) { band, startRow, rows ->
            bitmap.getPixels(band, 0, w, 0, startRow, w, rows)
        }
    }
}
