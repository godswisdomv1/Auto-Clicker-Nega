package com.zdmgold.autoclickernega.lab

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zdmgold.autoclickernega.ui.components.NavBar
import com.zdmgold.autoclickernega.ui.components.NavBarBack
import com.zdmgold.autoclickernega.ui.components.NegaCard
import com.zdmgold.autoclickernega.ui.components.NegaText
import com.zdmgold.autoclickernega.ui.components.PrimaryButton
import com.zdmgold.autoclickernega.ui.components.SecondaryButton
import com.zdmgold.autoclickernega.ui.components.SectionHeader
import com.zdmgold.autoclickernega.ui.theme.NegaTheme
import com.zdmgold.autoclickernega.ui.theme.Tokens

/**
 * The Detection lab, in debug builds only. Each button waits five seconds before it captures, so there is time to
 * switch to the app being measured. The lab text is English only, since it is a developer tool.
 */
@Composable
fun DetectionLabScreen(
    viewModel: DetectionLabViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val idle = state.busy == null
    Column(Modifier.fillMaxSize()) {
        NavBar(title = "Detection lab", leading = { NavBarBack(onBack) })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Tokens.Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.s),
        ) {
            Status(state)
            SectionHeader("Quick test")
            NegaText(
                "Tap, then switch to a screen that stays still. After 5 seconds the lab picks a detailed patch " +
                    "of it and looks for that patch 100 times.",
                color = NegaTheme.colors.textSecondary,
            )
            PrimaryButton("Quick test (100 captures)", viewModel::runQuickTest, Modifier.fillMaxWidth(), enabled = idle)
            SectionHeader("Manual: 1. Capture")
            PrimaryButton(
                "Capture reference image",
                viewModel::captureReference,
                Modifier.fillMaxWidth(),
                enabled = idle,
            )
            PrimaryButton("Capture test frame", viewModel::captureFrame, Modifier.fillMaxWidth(), enabled = idle)
            SectionHeader("Manual: 2. Measure")
            SecondaryButton("Match at each confidence", viewModel::runMatch, Modifier.fillMaxWidth(), enabled = idle)
            SecondaryButton("Repeat test (100 captures)", viewModel::runRepeat, Modifier.fillMaxWidth(), enabled = idle)
            SecondaryButton("Screenshot rate probe", viewModel::runRateProbe, Modifier.fillMaxWidth(), enabled = idle)
            if (!idle) SecondaryButton("Cancel", viewModel::cancel, Modifier.fillMaxWidth(), destructive = true)
            SectionHeader("Report")
            SecondaryButton(
                "Share report",
                {
                    val send =
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, viewModel.report())
                        }
                    context.startActivity(
                        Intent.createChooser(send, "Share report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
                Modifier.fillMaxWidth(),
                enabled = idle,
            )
            Results(viewModel.report())
        }
    }
}

@Composable
private fun Status(state: LabState) {
    val lines =
        listOfNotNull(
            state.busy?.let { "Working: $it" },
            state.countdown?.let { "Switch to the app now. Capturing in $it" },
            state.progress,
            state.message,
            state.reference?.let { "Reference: ${it.text}" },
            state.frameText?.let { "Test frame: $it" },
        )
    if (lines.isEmpty()) return
    NegaCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Tokens.Spacing.m), verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)) {
            lines.forEach { NegaText(it, style = NegaTheme.typography.callout) }
        }
    }
}

@Composable
private fun Results(report: String) {
    NegaCard(Modifier.fillMaxWidth()) {
        NegaText(report, style = NegaTheme.typography.mono, modifier = Modifier.padding(Tokens.Spacing.m))
    }
}
