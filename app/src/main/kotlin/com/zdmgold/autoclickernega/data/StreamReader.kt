package com.zdmgold.autoclickernega.data

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.error.ImportProblem
import com.zdmgold.autoclickernega.core.result.Outcome
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

private const val BUFFER = 8192

/** Reads a stream but stops, with a typed error, as soon as it passes [limit] bytes. */
internal fun readCapped(
    stream: InputStream,
    limit: Int,
): Outcome<ByteArray> =
    try {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER)
        var result: Outcome<ByteArray>? = null
        while (result == null) {
            val n = stream.read(buffer)
            when {
                n < 0 -> result = Outcome.Success(out.toByteArray())
                out.size() + n > limit -> result = Outcome.Failure(AppError.MalformedImport(ImportProblem.TooLarge))
                else -> out.write(buffer, 0, n)
            }
        }
        result
    } catch (e: IOException) {
        Outcome.Failure(AppError.Storage(e.message.orEmpty()))
    }
