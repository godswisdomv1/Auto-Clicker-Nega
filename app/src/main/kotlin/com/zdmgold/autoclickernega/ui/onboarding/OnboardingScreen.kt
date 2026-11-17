package com.zdmgold.autoclickernega.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zdmgold.autoclickernega.R
import com.zdmgold.autoclickernega.core.model.ServiceStatus
import com.zdmgold.autoclickernega.platform.access.SettingsIntents
import com.zdmgold.autoclickernega.ui.components.NavBar
import com.zdmgold.autoclickernega.ui.components.NegaCard
import com.zdmgold.autoclickernega.ui.components.NegaRow
import com.zdmgold.autoclickernega.ui.components.NegaText
import com.zdmgold.autoclickernega.ui.components.NegaWordmark
import com.zdmgold.autoclickernega.ui.components.PrimaryButton
import com.zdmgold.autoclickernega.ui.components.SecondaryButton
import com.zdmgold.autoclickernega.ui.icons.AppIcon
import com.zdmgold.autoclickernega.ui.icons.AppIcons
import com.zdmgold.autoclickernega.ui.theme.NegaTheme
import com.zdmgold.autoclickernega.ui.theme.Tokens

/**
 * First launch: a welcome, the prominent disclosure, and the way to the system settings with the live state of the
 * service. Declining the disclosure leaves for Home without marking onboarding done, so it is offered again later.
 */
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onDecline: () -> Unit,
    onFinish: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(enabled = state.step == OnboardingStep.Disclosure) { viewModel.onBack() }
    Column(Modifier.fillMaxSize()) {
        NavBar(leading = { NegaWordmark(Modifier.padding(start = Tokens.Spacing.s)) })
        Column(Modifier.fillMaxSize().padding(horizontal = Tokens.Spacing.m)) {
            when (state.step) {
                OnboardingStep.Welcome -> WelcomeStep(viewModel::onContinue)
                OnboardingStep.Disclosure -> DisclosureContent(Modifier.fillMaxSize(), viewModel::onAgree, onDecline)
                OnboardingStep.EnableService -> EnableStep(state.service, onFinish)
            }
        }
    }
}

@Composable
private fun WelcomeStep(onContinue: () -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.m)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.m, Alignment.CenterVertically),
        ) {
            AppIcon(AppIcons.Tap, size = Tokens.IconSize.hero, tint = NegaTheme.colors.accentText)
            NegaText(
                stringResource(R.string.onboarding_welcome_title),
                style = NegaTheme.typography.headline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            NegaText(
                stringResource(R.string.onboarding_welcome_body),
                color = NegaTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
        PrimaryButton(
            stringResource(R.string.action_continue),
            onContinue,
            Modifier.fillMaxWidth().padding(bottom = Tokens.Spacing.m),
        )
    }
}

@Composable
private fun EnableStep(
    service: ServiceStatus,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val ready = service == ServiceStatus.Ready
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.m)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.m),
        ) {
            AppIcon(
                if (ready) AppIcons.Check else AppIcons.Shield,
                size = Tokens.IconSize.hero,
                tint = if (ready) NegaTheme.colors.success else NegaTheme.colors.accentText,
            )
            NegaText(
                stringResource(if (ready) R.string.onboarding_ready_title else R.string.onboarding_enable_title),
                style = NegaTheme.typography.headline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            NegaText(
                stringResource(if (ready) R.string.onboarding_ready_body else R.string.onboarding_enable_body),
                color = NegaTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            NegaCard(Modifier.fillMaxWidth()) {
                NegaRow(title = stringResource(serviceText(service)))
            }
        }
        if (ready) {
            PrimaryButton(
                stringResource(R.string.onboarding_go_home),
                onFinish,
                Modifier.fillMaxWidth().padding(bottom = Tokens.Spacing.m),
            )
        } else {
            PrimaryButton(
                stringResource(R.string.action_open_accessibility),
                { SettingsIntents.open(context, SettingsIntents.accessibility()) },
                Modifier.fillMaxWidth(),
            )
            SecondaryButton(
                stringResource(R.string.onboarding_later),
                onFinish,
                Modifier.fillMaxWidth().padding(bottom = Tokens.Spacing.m),
            )
        }
    }
}

private fun serviceText(service: ServiceStatus): Int =
    when (service) {
        ServiceStatus.Ready -> R.string.status_service_ready
        ServiceStatus.Off -> R.string.status_service_off
        ServiceStatus.Connecting -> R.string.status_service_connecting
    }
