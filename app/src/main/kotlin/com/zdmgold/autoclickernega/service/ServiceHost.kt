package com.zdmgold.autoclickernega.service

import com.zdmgold.autoclickernega.core.port.ScriptStore
import com.zdmgold.autoclickernega.core.port.SettingsStore
import com.zdmgold.autoclickernega.core.port.TemplateStore
import com.zdmgold.autoclickernega.core.port.TextRecognizer

/**
 * What the accessibility service needs from the app, handed over when the app starts. Each is given as a provider and
 * built on first use, so the app starts without opening the database.
 */
class ServiceDependencies(
    scriptsProvider: () -> ScriptStore,
    templatesProvider: () -> TemplateStore,
    settingsProvider: () -> SettingsStore,
    val recognizer: TextRecognizer?,
) {
    val scripts: ScriptStore by lazy(scriptsProvider)
    val templates: TemplateStore by lazy(templatesProvider)
    val settings: SettingsStore by lazy(settingsProvider)
}

/** The app puts its dependencies here at startup. The service may be created before or after, so it checks. */
object ServiceHost {
    @Volatile
    var dependencies: ServiceDependencies? = null
}
