package com.zdmgold.autoclickernega.ui

/**
 * Which screens are built yet. Home shows an entry only when its screen exists, so a tester never lands on a dead
 * end. Each later batch switches its own flag on in the same commit that adds the screen.
 */
object Features {
    const val EDITOR: Boolean = true
    const val QUICK_SCENARIO: Boolean = false
    const val SETTINGS: Boolean = false
}
