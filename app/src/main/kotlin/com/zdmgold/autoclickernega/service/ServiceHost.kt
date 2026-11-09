package com.zdmgold.autoclickernega.service

import com.zdmgold.autoclickernega.core.port.ScriptStore
import com.zdmgold.autoclickernega.core.port.SettingsStore
import com.zdmgold.autoclickernega.core.port.TemplateStore
import com.zdmgold.autoclickernega.core.port.TextRecognizer

/** What the accessibility service needs from the app, handed over when the app starts. */
class ServiceDependencies(
    val scripts: ScriptStore,
    val templates: TemplateStore,
    val settings: SettingsStore,
    val recognizer: TextRecognizer?,
)

/** The app puts its dependencies here at startup. The service may be created before or after, so it checks. */
object ServiceHost {
    @Volatile
    var dependencies: ServiceDependencies? = null
}
