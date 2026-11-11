package com.zdmgold.autoclickernega.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.zdmgold.autoclickernega.core.model.Comparison
import com.zdmgold.autoclickernega.core.model.Condition
import com.zdmgold.autoclickernega.core.model.Rect
import com.zdmgold.autoclickernega.core.model.ScreenSpec
import com.zdmgold.autoclickernega.ui.icons.AppIcons
import com.zdmgold.autoclickernega.ui.theme.NegaTheme
import com.zdmgold.autoclickernega.ui.theme.Tokens

/** Every component on one page, in light and dark, so a change to a token or a component can be judged at a glance. */
@Composable
private fun Gallery(dark: Boolean) {
    NegaTheme(darkTheme = dark) {
        Column(
            modifier =
                Modifier
                    .background(NegaTheme.colors.bgPrimary)
                    .padding(Tokens.Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.m),
        ) {
            NavBar(title = "Settings", leading = { NegaWordmark() }) {
                ChipIconButton(AppIcons.Settings, "Settings", {})
            }
            SectionHeader("Scripts")
            NegaCard {
                NegaRow("Collect reward", subtitle = "Smart · 2 events", icon = AppIcons.Tap, onClick = {})
                NegaRow("Tap loop", subtitle = "Simple · 4 steps", icon = AppIcons.Repeat, onClick = {})
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.s)) {
                NegaChip("Safe mode", icon = AppIcons.Shield)
                ConditionChip(Condition.Image("t1"))
                ConditionChip(Condition.Counter("c1", Comparison.Lt, 50, negate = true))
            }
            PrimaryButton("Open floating panel", {}, Modifier.fillMaxWidth(), icon = AppIcons.Play)
            SecondaryButton("Delete script", {}, Modifier.fillMaxWidth(), destructive = true)
            InteractiveParts()
            UndoBanner("Script deleted", "Undo", {})
            EmptyState(
                title = "No scripts yet",
                message = "Make your first one with Quick scenario.",
                icon = AppIcons.Tap,
            )
        }
    }
}

@Composable
private fun InteractiveParts() {
    var text by remember { mutableStateOf("") }
    var on by remember { mutableStateOf(true) }
    var count by remember { mutableIntStateOf(3) }
    var choice by remember { mutableIntStateOf(0) }
    var confidence by remember { mutableFloatStateOf(0.85f) }
    var region by remember { mutableStateOf<Rect?>(Rect(100, 600, 900, 1500)) }
    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.m)) {
        NegaTextField(text, { text = it }, label = "Name", placeholder = "Untitled")
        NegaTextField("x", {}, label = "Interval", errorText = "Use at least 50 ms")
        Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.m)) {
            NegaToggle(on, { on = it }, description = "Haptics")
            NegaToggle(checked = false, onCheckedChange = {}, enabled = false)
            Stepper(count, { count = it }, 0..10)
        }
        Segmented(listOf("Auto", "Fast", "Battery"), choice, { choice = it })
        ConfidenceSlider(confidence, { confidence = it })
        RegionPicker(ScreenSpec(1080, 2400, 420), region, { region = it })
    }
}

@Preview(name = "Components light", showBackground = true, widthDp = 380, heightDp = 1800)
@Composable
private fun GalleryLight() = Gallery(dark = false)

@Preview(name = "Components dark", showBackground = true, widthDp = 380, heightDp = 1800)
@Composable
private fun GalleryDark() = Gallery(dark = true)

@Preview(name = "Sheet and dialog", showBackground = true, widthDp = 380, heightDp = 640)
@Composable
private fun OverlaysPreview() {
    NegaTheme(darkTheme = false) {
        NegaScreen {
            NegaSheet(visible = true, onDismiss = {}) {
                NegaText("Watch a short video to start this run", style = NegaTheme.typography.title1)
                PrimaryButton("Watch video", {}, Modifier.fillMaxWidth())
                SecondaryButton("Get Pro", {}, Modifier.fillMaxWidth())
            }
            NegaDialog(
                visible = false,
                title = "Delete all local data?",
                confirmLabel = "Delete",
                onConfirm = {},
                onDismiss = {},
                dismissLabel = "Cancel",
                destructive = true,
            )
        }
    }
}

@Preview(name = "Error", showBackground = true, widthDp = 380, heightDp = 480)
@Composable
private fun ErrorPreview() {
    NegaTheme(darkTheme = true) { ErrorView(onRetry = {}, onReport = {}) }
}
