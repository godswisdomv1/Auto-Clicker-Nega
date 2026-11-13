package com.zdmgold.autoclickernega.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.zdmgold.autoclickernega.R
import com.zdmgold.autoclickernega.core.model.ServiceStatus
import com.zdmgold.autoclickernega.platform.access.SettingsIntents
import com.zdmgold.autoclickernega.ui.components.NavBar
import com.zdmgold.autoclickernega.ui.components.NegaCard
import com.zdmgold.autoclickernega.ui.components.NegaRow
import com.zdmgold.autoclickernega.ui.components.NegaWordmark
import com.zdmgold.autoclickernega.ui.components.PrimaryButton
import com.zdmgold.autoclickernega.ui.theme.Tokens

/**
 * What the app shows until the real screens arrive: the wordmark and the live state of the accessibility service.
 * It proves the shell, the theme, the service detection and the settings link work together.
 */
@Composable
fun ShellPlaceholder(state: AppUiState) {
    val context = LocalContext.current
    Column {
        NavBar(leading = { NegaWordmark(Modifier.padding(start = Tokens.Spacing.s)) })
        Column(Modifier.padding(Tokens.Spacing.m)) {
            NegaCard {
                NegaRow(
                    title =
                        stringResource(
                            when (state.service) {
                                ServiceStatus.Ready -> R.string.status_service_ready
                                ServiceStatus.Off -> R.string.status_service_off
                                ServiceStatus.Connecting -> R.string.status_service_connecting
                            },
                        ),
                )
            }
            if (state.service != ServiceStatus.Ready) {
                PrimaryButton(
                    label = stringResource(R.string.action_open_accessibility),
                    onClick = { SettingsIntents.open(context, SettingsIntents.accessibility()) },
                    modifier = Modifier.fillMaxWidth().padding(top = Tokens.Spacing.m),
                )
            }
        }
    }
}
