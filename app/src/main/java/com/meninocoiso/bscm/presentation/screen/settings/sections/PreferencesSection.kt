package com.meninocoiso.bscm.presentation.screen.settings.sections

import androidx.compose.material3.ListItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.model.internal.Settings
import com.meninocoiso.bscm.presentation.ui.components.SwitchUI
import com.meninocoiso.bscm.presentation.ui.components.settings.HeadlineText
import com.meninocoiso.bscm.presentation.ui.components.settings.ListDivider
import com.meninocoiso.bscm.presentation.ui.components.settings.SettingsCard
import com.meninocoiso.bscm.presentation.ui.components.settings.SupportingText
import com.meninocoiso.bscm.presentation.ui.components.settings.settingsCard

@Composable
fun PreferencesSection(
    uiState: Settings,
    allowExplicitContent: (Boolean) -> Unit,
    enableGameplayPreviewVideo: (Boolean) -> Unit,
) {
    SettingsCard(title = stringResource(R.string.preferences)) {
        ListItem(
            modifier = Modifier.settingsCard(),
            headlineContent = {
                HeadlineText(stringResource(R.string.explicit_content))
            },
            supportingContent = {
                SupportingText(
                    stringResource(R.string.explicit_content_description)
                )
            },
            trailingContent = {
                SwitchUI(
                    checked = uiState.allowExplicitContent,
                    onCheckedChange = allowExplicitContent
                )
            }
        )
        ListDivider()
        ListItem(
            modifier = Modifier.settingsCard(),
            headlineContent = {
                HeadlineText(stringResource(R.string.gameplay_preview))
            },
            supportingContent = {
                SupportingText(
                    stringResource(R.string.gameplay_preview_description)
                )
            },
            trailingContent = {
                SwitchUI(
                    checked = uiState.enableGameplayPreviewVideo,
                    onCheckedChange = enableGameplayPreviewVideo
                )
            }
        )
    }
}