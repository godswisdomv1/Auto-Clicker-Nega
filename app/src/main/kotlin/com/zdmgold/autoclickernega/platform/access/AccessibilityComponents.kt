package com.zdmgold.autoclickernega.platform.access

/** Reads the system's list of enabled accessibility services. */
object AccessibilityComponents {
    /**
     * True when [component], written as package/class, is in the colon-separated [setting]. The system may write a
     * class either in full or relative to its package, so both are accepted, and case is ignored.
     */
    fun isEnabled(
        setting: String?,
        component: String,
    ): Boolean {
        if (setting.isNullOrBlank()) return false
        val wanted = normalize(component)
        return setting.split(':').any { normalize(it) == wanted }
    }

    private fun normalize(entry: String): String {
        val trimmed = entry.trim().lowercase()
        val pkg = trimmed.substringBefore('/')
        val cls = trimmed.substringAfter('/', "")
        val full = if (cls.startsWith(".")) pkg + cls else cls
        return "$pkg/$full"
    }
}
