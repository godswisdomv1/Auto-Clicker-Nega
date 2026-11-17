package com.zdmgold.autoclickernega.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.zdmgold.autoclickernega.R
import com.zdmgold.autoclickernega.ui.components.NegaText
import com.zdmgold.autoclickernega.ui.components.PrimaryButton
import com.zdmgold.autoclickernega.ui.components.SecondaryButton
import com.zdmgold.autoclickernega.ui.icons.AppIcon
import com.zdmgold.autoclickernega.ui.icons.AppIcons
import com.zdmgold.autoclickernega.ui.theme.NegaTheme
import com.zdmgold.autoclickernega.ui.theme.Tokens

/**
 * The prominent disclosure about what the accessibility service does. It is one composable so onboarding and Help
 * show exactly the same words. When [onAgree] and [onDecline] are null only the text is shown.
 */
@Composable
fun DisclosureContent(
    modifier: Modifier = Modifier,
    onAgree: (() -> Unit)? = null,
    onDecline: (() -> Unit)? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.m)) {
        Column(
            modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.m),
        ) {
            AppIcon(AppIcons.Shield, size = Tokens.IconSize.hero, tint = NegaTheme.colors.accentText)
            NegaText(
                stringResource(R.string.disclosure_title),
                style = NegaTheme.typography.headline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            NegaText(
                stringResource(R.string.disclosure_body),
                color = NegaTheme.colors.textSecondary,
            )
        }
        if (onAgree != null) {
            PrimaryButton(stringResource(R.string.disclosure_agree), onAgree, Modifier.fillMaxWidth())
        }
        if (onDecline != null) {
            SecondaryButton(
                stringResource(R.string.disclosure_decline),
                onDecline,
                Modifier.fillMaxWidth().padding(bottom = Tokens.Spacing.s),
            )
        }
    }
}
