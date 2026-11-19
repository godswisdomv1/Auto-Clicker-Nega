package com.zdmgold.autoclickernega.core.lab

import java.util.Locale

/** One confidence level of a single-frame match run. */
data class ConfidenceRow(
    val confidence: Float,
    val accepted: Boolean,
)

/** A small template searched for in a region, which is what a typical script does. */
data class SmallBenchmark(
    val templateText: String,
    val regionText: String,
    val score: Float,
    val safeMedianMs: Long,
    val fastMedianMs: Long,
)

/** The result of matching the reference image against one captured frame. */
data class MatchRunResult(
    val score: Float,
    val boxText: String,
    val safeMedianMs: Long,
    val fastMedianMs: Long,
    val rows: List<ConfidenceRow>,
    val small: SmallBenchmark? = null,
)

/** The result of the repeat test: many captures of one screen, each matched against the reference. */
data class RepeatResult(
    val attempts: Int,
    val captured: Int,
    val refused: Int,
    val otherFailures: Int,
    val detections: Int,
    val confidence: Float,
    val matchMs: List<Long>,
    val captureToMatchMs: List<Long>,
    val elapsedMs: Long,
    val learnedMinGapMs: Long,
    val heapMb: List<Long> = emptyList(),
    val heapLimitMb: Long = 0,
    val scores: List<Float> = emptyList(),
    val atCutPosition: Int = 0,
)

/** The result of asking for screenshots as fast as the lab can, to find what the system accepts. */
data class RateResult(
    val attempts: Int,
    val accepted: Int,
    val refused: Int,
    val acceptedGapsMs: List<Long>,
    val spacingMs: Long,
)

/** The text of the shareable lab report. It holds numbers only, never pictures. */
object LabReport {
    fun build(
        device: String,
        android: String,
        appVersion: String,
        reference: String?,
        match: MatchRunResult?,
        repeat: RepeatResult?,
        rate: RateResult?,
    ): String =
        buildString {
            appendLine("Nega Detection lab report")
            appendLine("Device: $device")
            appendLine("Android: $android")
            appendLine("App: $appVersion")
            appendLine("Reference image: ${reference ?: "none captured"}")
            if (match != null) {
                appendLine()
                appendLine("Single frame match")
                appendLine("  best score ${score(match.score)} at ${match.boxText}")
                appendLine("  time per search: Safe ${match.safeMedianMs} ms, Fast ${match.fastMedianMs} ms (median)")
                match.rows.forEach {
                    appendLine("  confidence ${score(it.confidence)}: ${if (it.accepted) "found" else "not found"}")
                }
                match.small?.let {
                    appendLine(
                        "  typical script: ${it.templateText} template in a ${it.regionText} region, score ${score(
                            it.score,
                        )}",
                    )
                    appendLine("  time per search: Safe ${it.safeMedianMs} ms, Fast ${it.fastMedianMs} ms (median)")
                }
            }
            if (repeat != null) {
                appendLine()
                appendLine("Repeat test at confidence ${score(repeat.confidence)}")
                appendLine(
                    "  attempts ${repeat.attempts}, captured ${repeat.captured}, refused for rate ${repeat.refused}",
                )
                appendLine("  other capture failures ${repeat.otherFailures}")
                appendLine("  detections ${repeat.detections} of ${repeat.captured} captured frames")
                appendLine(
                    "  match time ms: median ${LabStats.median(
                        repeat.matchMs,
                    )}, p95 ${LabStats.percentile(repeat.matchMs, P95)}",
                )
                appendLine(
                    "  capture to result ms: median ${LabStats.median(repeat.captureToMatchMs)}, " +
                        "p95 ${LabStats.percentile(repeat.captureToMatchMs, P95)}",
                )
                appendLine("  searches per second ${perSecond(repeat.captured, repeat.elapsedMs)}")
                appendLine("  learned minimum gap ${repeat.learnedMinGapMs} ms")
                appendScores(repeat)
                if (repeat.heapMb.isNotEmpty()) {
                    appendLine(
                        "  Java heap MB every 10 attempts (limit ${repeat.heapLimitMb}): ${repeat.heapMb.joinToString(
                            " ",
                        )}",
                    )
                }
            }
            if (rate != null) {
                appendLine()
                appendLine("Screenshot rate probe (one request every ${rate.spacingMs} ms)")
                appendLine("  requests ${rate.attempts}, accepted ${rate.accepted}, refused ${rate.refused}")
                if (rate.acceptedGapsMs.isNotEmpty()) {
                    appendLine(
                        "  gap between accepted captures ms: min ${rate.acceptedGapsMs.min()}, median ${LabStats.median(
                            rate.acceptedGapsMs,
                        )}",
                    )
                }
            }
        }

    private fun StringBuilder.appendScores(repeat: RepeatResult) {
        if (repeat.scores.isEmpty()) return
        val sorted = repeat.scores.sorted()
        appendLine(
            "  best score per frame: first ${score(repeat.scores.first())}, min ${score(sorted.first())}, " +
                "median ${score(sorted[sorted.size / 2])}, max ${score(sorted.last())}",
        )
        appendLine(
            "  best match at the place the reference was cut: ${repeat.atCutPosition} of ${repeat.scores.size} frames",
        )
        appendLine(
            "  frames scoring below ${score(repeat.confidence)}: ${repeat.scores.count { it < repeat.confidence }}",
        )
        appendLine("  first scores: ${repeat.scores.take(FIRST_SCORES).joinToString(" ") { score(it) }}")
    }

    private fun score(value: Float) = String.format(Locale.ROOT, "%.2f", value)

    private fun perSecond(
        count: Int,
        elapsedMs: Long,
    ): String = if (elapsedMs <= 0) "0" else String.format(Locale.ROOT, "%.1f", count * MS_PER_SECOND / elapsedMs)

    private const val P95 = 95
    private const val FIRST_SCORES = 12
    private const val MS_PER_SECOND = 1000.0
}
