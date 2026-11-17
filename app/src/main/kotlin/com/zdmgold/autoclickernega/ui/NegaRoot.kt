package com.zdmgold.autoclickernega.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zdmgold.autoclickernega.AppGraph
import com.zdmgold.autoclickernega.R
import com.zdmgold.autoclickernega.lab.LabEntry
import com.zdmgold.autoclickernega.platform.display.DisplayInfo
import com.zdmgold.autoclickernega.ui.components.EmptyState
import com.zdmgold.autoclickernega.ui.components.NavBar
import com.zdmgold.autoclickernega.ui.components.NavBarBack
import com.zdmgold.autoclickernega.ui.components.NegaScreen
import com.zdmgold.autoclickernega.ui.home.HomeScreen
import com.zdmgold.autoclickernega.ui.home.HomeViewModel
import com.zdmgold.autoclickernega.ui.icons.AppIcons
import com.zdmgold.autoclickernega.ui.nav.NegaNavHost
import com.zdmgold.autoclickernega.ui.nav.Route
import com.zdmgold.autoclickernega.ui.onboarding.OnboardingScreen
import com.zdmgold.autoclickernega.ui.onboarding.OnboardingViewModel
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
                NegaNavHost(stack = stack, onBack = vm::back) { route ->
                    RouteContent(route, graph, vm, state)
                }
            }
        }
    }
}

@Composable
private fun RouteContent(
    route: Route,
    graph: AppGraph,
    vm: AppViewModel,
    state: AppUiState,
) {
    val context = LocalContext.current
    when (route) {
        Route.Onboarding -> {
            val model: OnboardingViewModel =
                viewModel(factory = OnboardingViewModel.factory(graph) { vm.agreeDisclosure() })
            OnboardingScreen(model, onDecline = vm::declineOnboarding, onFinish = vm::finishOnboarding)
        }

        Route.Home -> {
            val model: HomeViewModel =
                viewModel(factory = HomeViewModel.factory(graph) { DisplayInfo.screenSpec(context) })
            HomeScreen(model, state, vm::navigate)
        }

        Route.Lab -> {
            LabEntry.Screen(graph, vm::back)
        }

        else -> {
            PendingScreen(vm::back)
        }
    }
}

@Composable
private fun PendingScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        NavBar(leading = { NavBarBack(onBack) })
        EmptyState(
            title = stringResource(R.string.pending_title),
            message = stringResource(R.string.pending_message),
            icon = AppIcons.Info,
        )
    }
}
