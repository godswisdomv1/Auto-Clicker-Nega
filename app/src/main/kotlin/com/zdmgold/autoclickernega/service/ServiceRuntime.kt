package com.zdmgold.autoclickernega.service

import android.accessibilityservice.AccessibilityService
import android.content.res.Configuration
import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.ThemeMode
import com.zdmgold.autoclickernega.core.port.RunRequest
import com.zdmgold.autoclickernega.core.port.ScriptResolver
import com.zdmgold.autoclickernega.core.port.TemplateProvider
import com.zdmgold.autoclickernega.core.result.Outcome
import com.zdmgold.autoclickernega.core.script.ScriptValidator
import com.zdmgold.autoclickernega.core.vision.DetectionLog
import com.zdmgold.autoclickernega.service.bridge.EngineBridge
import com.zdmgold.autoclickernega.service.capture.ScreenshotFrameSource
import com.zdmgold.autoclickernega.service.capture.StoreTemplateProvider
import com.zdmgold.autoclickernega.service.engine.EngineController
import com.zdmgold.autoclickernega.service.engine.GestureDispatcher
import com.zdmgold.autoclickernega.service.engine.MonotonicClock
import com.zdmgold.autoclickernega.service.engine.RunDeps
import com.zdmgold.autoclickernega.service.overlay.OverlayHost
import com.zdmgold.autoclickernega.service.overlay.ScreenMetrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Everything that lives while the accessibility service is connected: the engine, the capture, the overlay and the
 * link to the app. The service class only creates it on connect and closes it on unbind.
 */
class ServiceRuntime(
    private val service: AccessibilityService,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val metrics = ScreenMetrics(service)
    private val clock = MonotonicClock()
    private val log = DetectionLog()
    private val dependencies = ServiceHost.dependencies

    @Volatile
    private var needsColor = false
    private val frameSource = ScreenshotFrameSource(service) { needsColor }
    private var controller: EngineController? = null
    private var host: OverlayHost? = null
    private val screenReceiver = ScreenStateReceiver { controller?.onScreenOff() }

    fun start() {
        val deps = dependencies
        val templates = deps?.let { StoreTemplateProvider(it.templates) } ?: TemplateProvider { _, _ -> null }
        val resolver = ScriptResolver { id -> (deps?.scripts?.get(id) as? Outcome.Success)?.value }
        val engine =
            EngineController(
                RunDeps(
                    clock = clock,
                    gestures = GestureDispatcher(service),
                    frames = { color ->
                        needsColor = color
                        frameSource
                    },
                    recognizer = deps?.recognizer,
                    templates = templates,
                    resolver = resolver,
                    panelMask = { host?.panelBounds() },
                    log = log,
                ),
                scope,
            )
        val overlay =
            OverlayHost(
                service = service,
                scope = scope,
                engine = engine,
                frames = frameSource,
                drafts = ScriptSession,
                log = log,
                clock = clock,
                metrics = metrics,
                startRun = ::startDraft,
                onPanelMoved = ::savePosition,
                dark = isDark(ThemeMode.System),
            )
        controller = engine
        host = overlay
        deps?.let { d ->
            scope.launch {
                d.settings.settings.collect {
                    overlay.applySettings(it.panelOpacity, it.panelPosition, isDark(it.themeMode))
                }
            }
        }
        EngineBridge.register(engine)
        EngineBridge.registerOverlay(overlay)
        screenReceiver.register(service)
    }

    fun onConfigurationChanged() {
        host?.onConfigurationChanged()
    }

    fun close() {
        EngineBridge.unregisterOverlay()
        EngineBridge.unregister()
        screenReceiver.unregister(service)
        controller?.onServiceLost()
        host?.release()
        frameSource.close()
        scope.cancel()
    }

    /** Runs the draft script built with the panel. The run gate joins this path with the ads batch. */
    private fun startDraft() {
        val draft = ScriptSession.draft.value ?: return
        val checked = ScriptValidator.validate(draft)
        if (checked is Outcome.Success) controller?.start(RunRequest(checked.value, metrics.current()))
    }

    private fun savePosition(position: Point) {
        val store = dependencies?.settings ?: return
        scope.launch { store.updateSettings { it.copy(panelPosition = position) } }
    }

    private fun isDark(mode: ThemeMode): Boolean =
        when (mode) {
            ThemeMode.Dark -> {
                true
            }

            ThemeMode.Light -> {
                false
            }

            ThemeMode.System -> {
                (service.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
            }
        }
}
