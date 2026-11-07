package com.zdmgold.autoclickernega.core.run

import com.zdmgold.autoclickernega.core.model.Limits

/** Stops scripts that run other scripts from looping or nesting too deeply. The script that started the run counts as depth 0. */
class LoopGuard(
    private val maxDepth: Int = Limits.MAX_RUN_SCRIPT_DEPTH,
) {
    private val stack = ArrayDeque<String>()

    val depth: Int get() = (stack.size - 1).coerceAtLeast(0)

    /** Returns false, and enters nothing, when [scriptId] is already running or the nesting would be too deep. */
    fun enter(scriptId: String): Boolean {
        if (scriptId in stack || stack.size > maxDepth) return false
        stack.addLast(scriptId)
        return true
    }

    fun exit(scriptId: String) {
        if (stack.lastOrNull() == scriptId) stack.removeLast()
    }
}
