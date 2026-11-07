package com.zdmgold.autoclickernega.core.run

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.error.PackCause
import com.zdmgold.autoclickernega.core.model.BoolOp
import com.zdmgold.autoclickernega.core.model.Condition
import com.zdmgold.autoclickernega.core.model.Expr
import com.zdmgold.autoclickernega.core.model.MatchMode
import com.zdmgold.autoclickernega.core.model.Rect
import com.zdmgold.autoclickernega.core.model.TimerSince
import com.zdmgold.autoclickernega.core.ocr.NumberParser
import com.zdmgold.autoclickernega.core.ocr.TextBlock
import com.zdmgold.autoclickernega.core.ocr.TextMatcher
import com.zdmgold.autoclickernega.core.port.TextRecognizer
import com.zdmgold.autoclickernega.core.result.Outcome
import com.zdmgold.autoclickernega.core.vision.ColorMatcher
import com.zdmgold.autoclickernega.core.vision.Frame
import com.zdmgold.autoclickernega.core.vision.Match
import com.zdmgold.autoclickernega.core.vision.MatchStabilizer
import com.zdmgold.autoclickernega.core.vision.MissReason
import com.zdmgold.autoclickernega.core.vision.NccMatcher

/**
 * The result of evaluating an expression on one frame. [anchor] is the match that "tap on the match" actions
 * aim at, and is set only when the expression passed. [score] and [miss] feed the HUD.
 */
class Evaluation(
    val passed: Boolean,
    val anchor: Match?,
    val score: Float?,
    val miss: MissReason,
    val error: AppError? = null,
)

/**
 * Evaluates an expression cheapest first with short-circuiting: counters and timers, then colour, then image,
 * then text and numbers. Image matches in Safe mode must hold on consecutive frames, and text is read at most
 * once per 300 ms per region, since recognition is the slowest step.
 */
class ConditionEvaluator(
    private val templates: Map<String, Frame>,
    private val recognizer: TextRecognizer?,
) {
    private class Tracked(
        val stabilizer: MatchStabilizer = MatchStabilizer(),
        var lastFrame: Long = -1,
    )

    private class CachedRead(
        val atMs: Long,
        val outcome: Outcome<List<TextBlock>>,
    )

    private class One(
        val raw: Boolean,
        val match: Match? = null,
        val score: Float? = null,
        val miss: MissReason = MissReason.None,
        val error: AppError? = null,
    )

    private val stabilizers = HashMap<String, Tracked>()
    private val reads = HashMap<String, CachedRead>()

    /** [scope] names the event, so each condition keeps its own stabiliser and its own text cache. */
    suspend fun evaluate(
        expr: Expr,
        frame: Frame,
        ctx: RunContext,
        scope: String,
    ): Evaluation {
        var anchor: Match? = null
        var score: Float? = null
        var miss = MissReason.None
        var error: AppError? = null
        var result = expr.op == BoolOp.And
        for ((index, cond) in expr.items.withIndex().sortedBy { cost(it.value) }) {
            val one = evaluateOne(cond, frame, ctx, "$scope#$index")
            one.score?.let { score = it }
            val passed = one.raw != cond.negate
            if (!passed) {
                miss = one.miss
                error = one.error ?: error
            }
            if (passed && !cond.negate && anchor == null) anchor = one.match
            if (passed == (expr.op == BoolOp.Or)) {
                result = passed
                break
            }
        }
        return Evaluation(result, if (result) anchor else null, score, if (result) MissReason.None else miss, error)
    }

    private enum class Cost { Cheap, Colour, Image, Text }

    private fun cost(c: Condition): Cost =
        when (c) {
            is Condition.Counter, is Condition.Timer -> Cost.Cheap
            is Condition.Color -> Cost.Colour
            is Condition.Image -> Cost.Image
            is Condition.Text, is Condition.Number -> Cost.Text
        }

    private suspend fun evaluateOne(
        c: Condition,
        frame: Frame,
        ctx: RunContext,
        key: String,
    ): One =
        when (c) {
            is Condition.Counter -> One(c.cmp.holds(ctx.counters.get(c.id).toLong(), c.value.toLong()))
            is Condition.Timer -> timer(c, ctx)
            is Condition.Color -> color(c, frame)
            is Condition.Image -> image(c, frame, ctx, key)
            is Condition.Text -> text(c, frame, ctx, key)
            is Condition.Number -> number(c, frame, ctx, key)
        }

    private fun timer(
        c: Condition.Timer,
        ctx: RunContext,
    ): One {
        val now = ctx.clock.nowMs()
        val since =
            when (val s = c.since) {
                TimerSince.RunStart -> ctx.runStartMs
                is TimerSince.EventFired -> ctx.lastFiredAt(s.eventId)
            } ?: return One(false)
        return One(c.cmp.holds(now - since, c.ms))
    }

    private fun color(
        c: Condition.Color,
        frame: Frame,
    ): One {
        if (frame.rgb == null) return One(false, miss = MissReason.NoColorData)
        return One(ColorMatcher.matches(frame, c))
    }

    private fun image(
        c: Condition.Image,
        frame: Frame,
        ctx: RunContext,
        key: String,
    ): One {
        val template = templates[c.template] ?: return One(false, miss = MissReason.TemplateMissing)
        val region = (c.region ?: frame.bounds).intersect(frame.bounds)
        if (region.isEmpty || region.width < template.w || region.height < template.h) {
            return One(false, miss = MissReason.RegionTooSmall)
        }
        val result = NccMatcher.search(frame, template, c)
        val best = result.best
        val score = best?.score
        if (best == null) return One(false, score = null, miss = MissReason.TemplateTooFlat)
        if (c.mode == MatchMode.Fast) {
            val hit = result.accepted
            return One(hit != null, hit, score, if (hit != null) MissReason.None else MissReason.BelowConfidence)
        }
        val tracked = stabilizers.getOrPut(key) { Tracked() }
        if (tracked.lastFrame != ctx.frameIndex - 1 && tracked.lastFrame != ctx.frameIndex) tracked.stabilizer.reset()
        tracked.lastFrame = ctx.frameIndex
        val stable = tracked.stabilizer.accept(result.accepted)
        val miss =
            when {
                stable != null -> MissReason.None
                result.accepted == null -> MissReason.BelowConfidence
                else -> MissReason.NotStable
            }
        return One(stable != null, stable, score, miss)
    }

    private suspend fun text(
        c: Condition.Text,
        frame: Frame,
        ctx: RunContext,
        key: String,
    ): One {
        val region = (c.region ?: frame.bounds).intersect(frame.bounds)
        return when (val read = read(frame, region, c.lang, ctx, key)) {
            is Outcome.Failure -> {
                failure(read.error)
            }

            is Outcome.Success -> {
                val match = TextMatcher.find(read.value, c)
                One(
                    match != null,
                    match,
                    match?.score,
                    if (match !=
                        null
                    ) {
                        MissReason.None
                    } else {
                        MissReason.BelowConfidence
                    },
                )
            }
        }
    }

    private suspend fun number(
        c: Condition.Number,
        frame: Frame,
        ctx: RunContext,
        key: String,
    ): One {
        val region = c.region.intersect(frame.bounds)
        return when (val read = read(frame, region, c.lang, ctx, key)) {
            is Outcome.Failure -> {
                failure(read.error)
            }

            is Outcome.Success -> {
                val ordered = read.value.sortedWith(compareBy({ it.box.t }, { it.box.l }))
                val value = NumberParser.parse(ordered.joinToString(" ") { it.text })
                if (value == null) One(false, miss = MissReason.NoNumber) else One(c.cmp.holds(value, c.value))
            }
        }
    }

    private fun failure(error: AppError): One {
        val miss =
            if (error is AppError.LanguagePack && error.cause == PackCause.Missing) {
                MissReason.LanguagePackMissing
            } else {
                MissReason.CaptureUnavailable
            }
        return One(false, miss = miss, error = error)
    }

    private suspend fun read(
        frame: Frame,
        region: Rect,
        lang: String,
        ctx: RunContext,
        key: String,
    ): Outcome<List<TextBlock>> {
        val engine = recognizer ?: return Outcome.Failure(AppError.LanguagePack(PackCause.Missing))
        if (region.isEmpty) return Outcome.Success(emptyList())
        val now = ctx.clock.nowMs()
        val cacheKey = "$key|$region|$lang"
        val cached = reads[cacheKey]
        if (cached != null && now - cached.atMs < OCR_MIN_INTERVAL_MS) return cached.outcome
        val outcome = engine.read(frame, region, lang)
        reads[cacheKey] = CachedRead(now, outcome)
        return outcome
    }

    private companion object {
        const val OCR_MIN_INTERVAL_MS = 300L
    }
}
