package com.zdmgold.autoclickernega

import android.app.Application
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.zdmgold.autoclickernega.core.port.DraftStore
import com.zdmgold.autoclickernega.core.port.OverlayPort
import com.zdmgold.autoclickernega.core.port.PackStore
import com.zdmgold.autoclickernega.core.port.ScriptStore
import com.zdmgold.autoclickernega.core.port.ServiceBridge
import com.zdmgold.autoclickernega.core.port.SettingsStore
import com.zdmgold.autoclickernega.core.port.TemplateStore
import com.zdmgold.autoclickernega.core.port.WallClock
import com.zdmgold.autoclickernega.core.vision.DetectionEntry
import com.zdmgold.autoclickernega.data.DataStoreSettings
import com.zdmgold.autoclickernega.data.DataWiper
import com.zdmgold.autoclickernega.data.FilePackStore
import com.zdmgold.autoclickernega.data.FileTemplateStore
import com.zdmgold.autoclickernega.data.RoomScriptStore
import com.zdmgold.autoclickernega.data.ScriptFileGateway
import com.zdmgold.autoclickernega.data.db.NegaDatabase
import com.zdmgold.autoclickernega.platform.access.AccessibilityState
import com.zdmgold.autoclickernega.platform.diag.CrashLogStore
import com.zdmgold.autoclickernega.platform.diag.DeviceInfoProvider
import com.zdmgold.autoclickernega.platform.diag.DiagnosticReport
import com.zdmgold.autoclickernega.platform.diag.ErrorLog
import com.zdmgold.autoclickernega.platform.diag.GlobalErrorHandler
import com.zdmgold.autoclickernega.platform.locale.LocaleManager
import com.zdmgold.autoclickernega.service.ScriptSession
import com.zdmgold.autoclickernega.service.ServiceDependencies
import com.zdmgold.autoclickernega.service.ServiceHost
import com.zdmgold.autoclickernega.service.bridge.EngineBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

/** The wall clock, used for dates such as the daily run counters. */
private object SystemWallClock : WallClock {
    override fun epochMs(): Long = System.currentTimeMillis()

    override fun localDate(): String = LocalDate.now().toString()
}

/**
 * Builds the app's parts once and hands them out. There is no injection framework: each part is created on first
 * use, so starting the app opens no database and does no disk work on the main thread.
 */
class AppGraph(
    private val app: Application,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val templatesDir = File(app.filesDir, "templates")
    private val packsDir = File(app.filesDir, "packs")
    private val crashDir = File(app.filesDir, "crashes")
    private val opened = lazy { NegaDatabase.open(app) }

    val wallClock: WallClock = SystemWallClock
    val scripts: ScriptStore by lazy { RoomScriptStore(opened.value.database.scripts(), wallClock) }
    val templates: TemplateStore by lazy { FileTemplateStore(templatesDir) }
    val packs: PackStore by lazy { FilePackStore(packsDir) }
    val settings: SettingsStore by lazy {
        DataStoreSettings(
            PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                scope = scope,
                produceFile = { app.preferencesDataStoreFile("settings") },
            ),
        )
    }
    val files: ScriptFileGateway by lazy { ScriptFileGateway(app) }
    val crashes = CrashLogStore(crashDir)
    val errors = ErrorLog()
    val locale = LocaleManager()
    val drafts: DraftStore = ScriptSession
    val engine: ServiceBridge = EngineBridge
    val overlay: OverlayPort = EngineBridge
    val accessibility: AccessibilityState by lazy { AccessibilityState(app, EngineBridge.state) }

    /** True when the database could not be opened and a fresh one was made. Read after the database is first used. */
    val databaseRecovered: Boolean get() = opened.isInitialized() && opened.value.recovered

    private val wiper: DataWiper by lazy {
        DataWiper(
            clearSettings = { settings.clearAll() },
            closeDatabase = { if (opened.isInitialized()) opened.value.database.close() },
            databaseFiles = listOf(app.getDatabasePath(NegaDatabase.FILE_NAME)),
            directories = listOf(templatesDir, packsDir, crashDir),
        )
    }

    /** Installs the crash handler and shares the stores with the accessibility service. Does no disk work. */
    fun start() {
        GlobalErrorHandler.install(crashes)
        ServiceHost.dependencies =
            ServiceDependencies(
                scriptsProvider = { scripts },
                templatesProvider = { templates },
                settingsProvider = { settings },
                recognizer = null,
            )
        scope.launch { opened.value }
    }

    /** The shareable report. It holds no scripts and no pictures. */
    fun diagnosticReport(detections: List<DetectionEntry>): String =
        DiagnosticReport.build(DeviceInfoProvider.read(app), errors.counts(), detections, crashes.entries())

    /** Delete all local data. The app should be restarted afterwards, since the database is closed. */
    suspend fun deleteAllLocalData() = wiper.wipeAll()
}
