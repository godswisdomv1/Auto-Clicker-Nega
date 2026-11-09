package com.zdmgold.autoclickernega.service.overlay

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import com.zdmgold.autoclickernega.R
import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.format.formatDuration
import com.zdmgold.autoclickernega.core.model.EngineState
import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.Script
import com.zdmgold.autoclickernega.core.model.ScriptBody
import com.zdmgold.autoclickernega.core.port.Clock
import com.zdmgold.autoclickernega.core.port.CropResult
import com.zdmgold.autoclickernega.core.port.DraftStore
import com.zdmgold.autoclickernega.core.port.OverlayPort
import com.zdmgold.autoclickernega.core.port.ServiceBridge
import com.zdmgold.autoclickernega.core.result.Outcome
import com.zdmgold.autoclickernega.core.vision.DetectionLog
import com.zdmgold.autoclickernega.core.vision.MissReason
import com.zdmgold.autoclickernega.service.capture.ScreenshotFrameSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Locale

/**
 * Connects the overlay to the engine and the draft. It turns engine state into what the panel shows, turns the
 * panel's buttons into engine commands and marker edits, and keeps the draft script in step with the markers.
 */
class OverlayHost(
    private val service: AccessibilityService,
    private val scope: CoroutineScope,
    private val engine: ServiceBridge,
    private val frames: ScreenshotFrameSource,
    private val drafts: DraftStore,
    private val log: DetectionLog,
    private val clock: Clock,
    private val metrics: ScreenMetrics,
    private val startRun: () -> Unit,
    private val onPanelMoved: (Point) -> Unit,
    dark: Boolean,
) : OverlayPort {
    private val visible = MutableStateFlow(false)
    private val crops = MutableSharedFlow<CropResult>(extraBufferCapacity = CROP_BUFFER)
    private var controller: OverlayController? = null
    private var dark = dark
    private var markers: List<Marker> = emptyList()
    private var counterNames: Map<String, String> = emptyMap()
    private var opacity = DEFAULT_OPACITY
    private var position: Point? = null

    override val panelVisible: StateFlow<Boolean> = visible.asStateFlow()
    override val panelCrops: SharedFlow<CropResult> = crops.asSharedFlow()

    init {
        scope.launch { engine.state.collect { render() } }
        scope.launch { engine.counters.collect { render() } }
        scope.launch { drafts.draft.collect { onDraftChanged(it) } }
        scope.launch {
            while (isActive) {
                delay(TICK_MS)
                if (visible.value &&
                    engine.state.value.let { it is EngineState.Running || it is EngineState.Paused }
                ) {
                    render()
                }
            }
        }
    }

    /** The panel's rectangle, or null when it is hidden. Detection uses it to avoid seeing the panel. */
    fun panelBounds(): com.zdmgold.autoclickernega.core.model.Rect? = controller?.panelBounds()

    override fun showPanel(): Outcome<Unit> {
        val c = controller ?: createController().also { controller = it }
        c.showPanel(position, opacity)
        visible.value = c.isPanelShown
        onDraftChanged(drafts.draft.value)
        render()
        return Outcome.Success(Unit)
    }

    override fun hidePanel(): Outcome<Unit> {
        controller?.let {
            it.markers.hide()
            it.hidePanel()
        }
        visible.value = false
        return Outcome.Success(Unit)
    }

    /** New look or position from the settings. A change of light or dark rebuilds the windows. */
    fun applySettings(
        opacity: Float,
        position: Point?,
        dark: Boolean,
    ) {
        this.opacity = opacity
        this.position = position
        controller?.setOpacity(opacity)
        if (dark != this.dark) {
            this.dark = dark
            rebuildWindows()
        }
    }

    /** The screen rotated or changed size, so every window is rebuilt for the new size. */
    fun onConfigurationChanged() = rebuildWindows()

    fun release() {
        controller?.release()
        controller = null
        visible.value = false
    }

    override suspend fun captureCrop(): Outcome<CropResult?> {
        val c = controller ?: createController().also { controller = it }
        c.setHiddenForCapture(true)
        delay(HIDE_SETTLE_MS)
        val shot = frames.captureBitmap()
        c.setHiddenForCapture(false)
        val bitmap =
            when (shot) {
                is Outcome.Failure -> return shot
                is Outcome.Success -> shot.value
            }
        val area = c.pickRect()
        if (area == null) {
            bitmap.recycle()
            return Outcome.Success(null)
        }
        val result = withContext(Dispatchers.Default) { cut(bitmap, area) }
        bitmap.recycle()
        return result
    }

    private fun cut(
        bitmap: Bitmap,
        area: com.zdmgold.autoclickernega.core.model.Rect,
    ): Outcome<CropResult?> {
        val l = area.l.coerceIn(0, bitmap.width - 1)
        val t = area.t.coerceIn(0, bitmap.height - 1)
        val w = (area.r - l).coerceIn(1, bitmap.width - l)
        val h = (area.b - t).coerceIn(1, bitmap.height - t)
        val piece = Bitmap.createBitmap(bitmap, l, t, w, h)
        val out = ByteArrayOutputStream()
        val ok = piece.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, out)
        piece.recycle()
        return if (ok) {
            Outcome.Success(
                CropResult(
                    com.zdmgold.autoclickernega.core.model
                        .Rect(l, t, l + w, t + h),
                    out.toByteArray(),
                ),
            )
        } else {
            Outcome.Failure(AppError.Storage("could not encode the image"))
        }
    }

    private fun createController(): OverlayController {
        val theme = OverlayTheme(service, dark)
        val actions =
            PanelActions(
                onToggleRun = ::onToggleRun,
                onStop = { engine.stop() },
                onCancelWait = { engine.stop() },
                onAddTap = { addMarker { n, c, _ -> Markers.newTap(n, c) } },
                onAddSwipe = { addMarker { n, c, len -> Markers.newSwipe(n, c, len) } },
                onAddCurve = { addMarker { n, c, len -> Markers.newCurve(n, c, len) } },
                onAddImage = { scope.launch { takePanelCrop() } },
                onClose = { hidePanel() },
                onDrag = { _, _ -> },
                onDragEnd = {},
            )
        return OverlayController(service, theme, metrics, actions) { moved ->
            position = moved
            onPanelMoved(moved)
        }.also { it.markers.onChanged = ::onMarkersChanged }
    }

    private fun rebuildWindows() {
        val wasVisible = visible.value
        controller?.release()
        controller = null
        visible.value = false
        if (wasVisible) showPanel()
    }

    private fun onToggleRun() {
        when (engine.state.value) {
            is EngineState.Running -> engine.pause()
            is EngineState.Paused -> engine.resume()
            else -> startRun()
        }
    }

    private suspend fun takePanelCrop() {
        val result = captureCrop()
        val crop = (result as? Outcome.Success)?.value ?: return
        crops.emit(crop)
    }

    private fun addMarker(create: (number: Int, center: Point, length: Int) -> Marker) {
        val draft = drafts.draft.value ?: newDraft()
        val steps = (draft.body as? ScriptBody.Simple)?.steps ?: return
        val bounds = metrics.bounds()
        val stagger = (markers.size % STAGGER_STEPS) * DEFAULT_STAGGER_PX
        val center = Point(bounds.width() / 2 + stagger, bounds.height() / 2 + stagger)
        val next =
            markers + create(markers.size + 1, center, minOf(bounds.width(), bounds.height()) / MARKER_SPAN_DIVISOR)
        drafts.update { it.copy(body = ScriptBody.Simple(Markers.applyTo(steps, next))) }
    }

    private fun newDraft(): Script {
        val script =
            Script(
                id = "",
                name = service.getString(R.string.draft_default_name),
                screen = metrics.current(),
                body = ScriptBody.Simple(emptyList()),
            )
        drafts.begin(script)
        return script
    }

    private fun onMarkersChanged(edited: List<Marker>) {
        drafts.update { s ->
            val body = s.body as? ScriptBody.Simple ?: return@update s
            s.copy(body = ScriptBody.Simple(Markers.applyTo(body.steps, edited)))
        }
    }

    private fun onDraftChanged(script: Script?) {
        counterNames = script?.counters?.associate { it.id to it.name }.orEmpty()
        val steps = (script?.body as? ScriptBody.Simple)?.steps.orEmpty()
        val derived = Markers.fromSteps(steps)
        if (derived != markers) markers = derived
        render()
    }

    private fun render() {
        val c = controller ?: return
        if (!c.isPanelShown) return
        val state = engine.state.value
        val editing = state is EngineState.Idle || state is EngineState.Blocked
        if (editing) c.markers.show(markers) else c.markers.hide()
        c.setKeepScreenOn(state is EngineState.Waiting)
        c.render(PanelModel(state, statusText(state), countersText(), hudModel(state)))
    }

    private fun statusText(state: EngineState): String {
        val locale = service.resources.configuration.locales[0] ?: Locale.getDefault()
        return when (state) {
            is EngineState.Waiting -> {
                service.getString(R.string.panel_wait_remaining, formatDuration(state.remainingMs, locale))
            }

            is EngineState.Countdown -> {
                service.getString(R.string.panel_countdown, state.secondsLeft)
            }

            is EngineState.Running -> {
                service.getString(R.string.panel_elapsed, formatDuration(clock.nowMs() - state.startedAtMs, locale))
            }

            is EngineState.Paused -> {
                service.getString(R.string.panel_paused)
            }

            is EngineState.Blocked -> {
                service.getString(R.string.panel_blocked)
            }

            else -> {
                ""
            }
        }
    }

    private fun countersText(): String =
        engine.counters.value.entries
            .take(MAX_COUNTERS_SHOWN)
            .joinToString(" · ") { (id, value) -> "${counterNames[id] ?: id} $value" }

    private fun hudModel(state: EngineState): HudModel? {
        if (state !is EngineState.Running && state !is EngineState.Paused) return null
        val last = log.last() ?: return HudModel(null, MissReason.None)
        return HudModel(last.score, last.miss)
    }

    private companion object {
        const val CROP_BUFFER = 4
        const val DEFAULT_OPACITY = 0.9f
        const val TICK_MS = 500L
        const val HIDE_SETTLE_MS = 150L
        const val PNG_QUALITY = 100
        const val DEFAULT_STAGGER_PX = 40
        const val STAGGER_STEPS = 5
        const val MAX_COUNTERS_SHOWN = 3
        const val MARKER_SPAN_DIVISOR = 3
    }
}
