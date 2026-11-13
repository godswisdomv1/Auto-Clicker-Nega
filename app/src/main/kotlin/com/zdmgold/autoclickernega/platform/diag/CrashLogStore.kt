package com.zdmgold.autoclickernega.platform.diag

import com.zdmgold.autoclickernega.core.json.Json
import com.zdmgold.autoclickernega.core.json.JsonArray
import com.zdmgold.autoclickernega.core.json.JsonNumber
import com.zdmgold.autoclickernega.core.json.JsonObject
import com.zdmgold.autoclickernega.core.json.JsonParseException
import com.zdmgold.autoclickernega.core.json.JsonString
import com.zdmgold.autoclickernega.core.json.encode
import com.zdmgold.autoclickernega.core.json.jsonArray
import com.zdmgold.autoclickernega.core.json.jsonObject
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/** One saved crash: when, on which thread, what was thrown and where. */
data class CrashEntry(
    val timeMs: Long,
    val thread: String,
    val type: String,
    val message: String,
    val stack: List<String>,
)

/**
 * Keeps the last five crashes in app storage, so a crash can be reported even though the app has no analytics. The
 * message is cut short and the stack is limited, and nothing here ever holds a script or a picture.
 */
class CrashLogStore(
    private val dir: File,
    private val now: () -> Long = System::currentTimeMillis,
    private val keep: Int = KEEP,
) {
    /** Saves a crash. This runs while the app is dying, so it never throws. */
    fun record(
        error: Throwable,
        threadName: String,
    ) {
        try {
            dir.mkdirs()
            val entry =
                CrashEntry(
                    timeMs = now(),
                    thread = threadName,
                    type = error.javaClass.name,
                    message = error.message.orEmpty().take(MESSAGE_LIMIT),
                    stack = error.stackTrace.take(STACK_LIMIT).map { it.toString() },
                )
            File(dir, "crash-${entry.timeMs}-${counter.incrementAndGet()}.json").writeText(encode(entry))
            prune()
        } catch (_: IOException) {
            // Nothing more can be done while crashing.
        } catch (_: SecurityException) {
            // Same.
        }
    }

    /** Saved crashes, newest first. A file that cannot be read is skipped. */
    fun entries(): List<CrashEntry> = files().mapNotNull { read(it) }

    fun clear() {
        files().forEach { it.delete() }
    }

    private fun files(): List<File> =
        dir
            .listFiles { f -> f.isFile && f.name.startsWith("crash-") }
            .orEmpty()
            .sortedWith(compareByDescending<File> { stamp(it) }.thenByDescending { it.name })

    private fun stamp(f: File): Long =
        f.name
            .removePrefix("crash-")
            .substringBefore('-')
            .toLongOrNull() ?: 0L

    private fun prune() {
        files().drop(keep).forEach { it.delete() }
    }

    private fun encode(e: CrashEntry): String =
        jsonObject(
            "timeMs" to JsonNumber.of(e.timeMs),
            "thread" to JsonString(e.thread),
            "type" to JsonString(e.type),
            "message" to JsonString(e.message),
            "stack" to jsonArray(e.stack.map { JsonString(it) }),
        ).encode()

    private fun read(file: File): CrashEntry? {
        val root =
            try {
                Json.parse(file.readText()) as? JsonObject
            } catch (e: JsonParseException) {
                null.also { e.message }
            } catch (_: IOException) {
                null
            } ?: return null
        val time = (root["timeMs"] as? JsonNumber)?.toLongOrNull() ?: return null
        val stack = (root["stack"] as? JsonArray)?.items?.mapNotNull { (it as? JsonString)?.value }.orEmpty()
        return CrashEntry(
            timeMs = time,
            thread = (root["thread"] as? JsonString)?.value.orEmpty(),
            type = (root["type"] as? JsonString)?.value.orEmpty(),
            message = (root["message"] as? JsonString)?.value.orEmpty(),
            stack = stack,
        )
    }

    private companion object {
        const val KEEP = 5
        const val MESSAGE_LIMIT = 200
        const val STACK_LIMIT = 40
        val counter = AtomicInteger()
    }
}
