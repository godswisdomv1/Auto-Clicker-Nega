package com.zdmgold.autoclickernega.core.script

import com.zdmgold.autoclickernega.core.model.Condition
import com.zdmgold.autoclickernega.core.model.Script
import com.zdmgold.autoclickernega.core.model.ScriptBody

/** True when any event looks at colour, so capture keeps full colour for this run and drops it otherwise. */
fun Script.usesColor(): Boolean =
    (body as? ScriptBody.Smart)
        ?.events
        .orEmpty()
        .any { event -> event.whenExpr.items.any { it is Condition.Color } }
