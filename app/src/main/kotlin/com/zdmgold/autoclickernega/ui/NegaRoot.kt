package com.zdmgold.autoclickernega.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zdmgold.autoclickernega.AppGraph
import com.zdmgold.autoclickernega.ui.components.NegaScreen
import com.zdmgold.autoclickernega.ui.nav.NegaNavHost
import com.zdmgold.autoclickernega.ui.theme.NegaTheme
import com.zdmgold.autoclickernega.ui.theme.ThemeResolver

/** The root of the app: the theme the user chose, the language, and the screen on top of the back stack. */
@Composable
fun NegaRoot(graph: AppGraph) {
    val vm: AppViewModel = viewModel(factory = AppViewModel.factory(graph))
    val state by vm.state.collectAsStateWithLifecycle()
    val stack by vm.stack.collectAsStateWithLifecycle()
    LaunchedEffect(state.loaded, state.languageTag) {
        if (state.loaded && graph.locale.current() != state.languageTag) graph.locale.apply(state.languageTag)
    }
    NegaTheme(darkTheme = ThemeResolver.isDark(state.themeMode, isSystemInDarkTheme())) {
        NegaScreen {
            if (state.loaded) {
                NegaNavHost(stack = stack, onBack = vm::back) { ShellPlaceholder(state) }
            }
        }
    }
}
