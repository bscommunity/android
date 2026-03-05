package com.meninocoiso.bscm.presentation.screen.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ListItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.enums.ThemePreference
import com.meninocoiso.bscm.domain.model.internal.Settings
import com.meninocoiso.bscm.presentation.ui.components.SwitchUI
import com.meninocoiso.bscm.presentation.ui.components.dialog.LanguageDialog
import com.meninocoiso.bscm.presentation.ui.components.dialog.ThemeDialog
import com.meninocoiso.bscm.presentation.ui.components.settings.HeadlineText
import com.meninocoiso.bscm.presentation.ui.components.settings.ListDivider
import com.meninocoiso.bscm.presentation.ui.components.settings.SettingsCard
import com.meninocoiso.bscm.presentation.ui.components.settings.SupportingText
import com.meninocoiso.bscm.presentation.ui.components.settings.settingsCard

@Composable
fun CustomizationSection(
    uiState: Settings,
    useDynamicColors: (Boolean) -> Unit,
    updateAppTheme: (ThemePreference) -> Unit
) {
    SettingsCard(title = stringResource(R.string.customization)) {
        ListItem(
            modifier = Modifier.settingsCard(),
            headlineContent = {
                HeadlineText(stringResource(R.string.material_you))
            },
            supportingContent = {
                SupportingText(
                    stringResource(R.string.material_you_description)
                )
            },
            trailingContent = {
                SwitchUI(
                    checked = uiState.useDynamicColors,
                    onCheckedChange = useDynamicColors
                )
            }
        )
        ListDivider()
        ListItem(
            modifier = Modifier.settingsCard(),
            headlineContent = {
                HeadlineText(stringResource(R.string.theme))
            },
            supportingContent = {
                SupportingText(
                    stringResource(R.string.theme_description)
                )
            },
            trailingContent = {
                ThemeDialog(
                    option = uiState.theme,
                    onThemeSelected = updateAppTheme,
                    onCancel = updateAppTheme
                )
            }
        )
        ListDivider()
        ListItem(
            modifier = Modifier.settingsCard(),
            headlineContent = {
                HeadlineText(stringResource(R.string.language))
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SupportingText(
                        stringResource(R.string.language_description)
                    )
                }
            },
            trailingContent = {
                LanguageDialog()
            }
        )
    }
}