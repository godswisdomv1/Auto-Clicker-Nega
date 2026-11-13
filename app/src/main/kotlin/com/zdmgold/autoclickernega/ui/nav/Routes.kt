package com.zdmgold.autoclickernega.ui.nav

/** Every screen, as a typed value. There are no exported deep links, so nothing outside the app can open a screen. */
sealed interface Route {
    data object Onboarding : Route

    data object Home : Route

    /** The editor for one script, or a new one when [scriptId] is null. */
    data class Editor(
        val scriptId: String?,
    ) : Route

    data object QuickScenario : Route

    data object Settings : Route

    data object Help : Route

    data object About : Route

    data object Paywall : Route

    /** The Detection lab, which only exists in debug builds. */
    data object Lab : Route
}

/** Which way the last move went, so the screen can slide in or out accordingly. */
enum class NavDirection { Push, Pop, Reset }

/**
 * The back stack as a plain value. Home is the root once onboarding is done, and the first launch starts on
 * onboarding, which is replaced rather than stacked so Back never returns to it.
 */
data class BackStack(
    val items: List<Route> = emptyList(),
    val direction: NavDirection = NavDirection.Reset,
) {
    val current: Route? get() = items.lastOrNull()
    val canPop: Boolean get() = items.size > 1

    /** Opens a screen. Opening the one already on top is ignored, so a double tap does not stack it twice. */
    fun push(route: Route): BackStack = if (current == route) this else BackStack(items + route, NavDirection.Push)

    fun pop(): BackStack = if (canPop) BackStack(items.dropLast(1), NavDirection.Pop) else this

    /** Replaces the whole stack with one screen. */
    fun reset(route: Route): BackStack = BackStack(listOf(route), NavDirection.Reset)

    companion object {
        /** The first screen: onboarding until it has been completed, then Home. */
        fun start(onboardingDone: Boolean): BackStack =
            BackStack(listOf(if (onboardingDone) Route.Home else Route.Onboarding), NavDirection.Reset)
    }
}
