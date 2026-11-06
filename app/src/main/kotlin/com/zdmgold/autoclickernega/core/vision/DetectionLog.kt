package com.zdmgold.autoclickernega.core.vision

import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.Rect

/** Why a search did not produce a tap. The HUD turns this into a short message. */
enum class MissReason {
    None,
    BelowConfidence,
    NotStable,
    RegionTooSmall,
    TemplateTooFlat,
    CaptureUnavailable,
    LanguagePackMissing,
    NoColorData,
}

/** How a gesture that followed a detection ended. */
enum class GestureOutcome { None, Completed, Cancelled, Rejected }

/** One detection. It records where and how well, and never any pixels. */
data class DetectionEntry(
    val frameTimeMs: Long,
    val eventId: String?,
    val score: Float?,
    val box: Rect?,
    val tap: Point?,
    val miss: MissReason,
    val gesture: GestureOutcome,
    val searchMs: Long,
)

/** The most recent detections, newest last. It feeds the HUD, the Detection lab and the diagnostic report. */
class DetectionLog(
    private val capacity: Int = DEFAULT_CAPACITY,
) {
    private val items = ArrayDeque<DetectionEntry>()

    init {
        require(capacity > 0) { "capacity must be positive" }
    }

    @Synchronized
    fun add(entry: DetectionEntry) {
        if (items.size == capacity) items.removeFirst()
        items.addLast(entry)
    }

    @Synchronized
    fun snapshot(): List<DetectionEntry> = items.toList()

    @Synchronized
    fun last(): DetectionEntry? = items.lastOrNull()

    @Synchronized
    fun clear() {
        items.clear()
    }

    companion object {
        const val DEFAULT_CAPACITY = 200
    }
}
