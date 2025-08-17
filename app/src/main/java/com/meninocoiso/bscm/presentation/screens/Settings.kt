package com.meninocoiso.bscm.presentation.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.bscm.BuildConfig
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.presentation.ui.components.SwitchUI
import com.meninocoiso.bscm.presentation.ui.components.dialog.LanguageDialog
import com.meninocoiso.bscm.presentation.ui.components.dialog.ThemeDialog
import com.meninocoiso.bscm.presentation.ui.modifiers.fabScrollObserver
import com.meninocoiso.bscm.presentation.ui.modifiers.rememberFabNestedScrollConnection
import com.meninocoiso.bscm.presentation.viewmodel.AppUpdateState
import com.meninocoiso.bscm.presentation.viewmodel.SettingsViewModel
import com.meninocoiso.bscm.util.LinkingUtils
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SettingsScreen(
    onFabStateChange: (Boolean) -> Unit,
    onSnackbar: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val shrunkVersionName = BuildConfig.VERSION_NAME.substringBeforeLast("-")
    val shrunkLatestVersion = if (updateState is AppUpdateState.UpdateAvailable)
        (updateState as AppUpdateState.UpdateAvailable).version.substringBeforeLast("-")
    else ""

    LaunchedEffect(updateState) {
        when (updateState) {
            is AppUpdateState.UpToDate -> {
                onSnackbar(context.getString(R.string.up_to_date))
            }

            is AppUpdateState.Error ->
                onSnackbar((updateState as AppUpdateState.Error).message)

            else -> {}
        }
    }

    val featureNotImplementedString = stringResource(R.string.feature_not_implemented)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(rememberFabNestedScrollConnection(onFabStateChange))
            .verticalScroll(rememberScrollState())
            .fabScrollObserver { onFabStateChange(it) }
            .padding(start = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 64.dp)
        ) {
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.displayMedium
            )
        }

        SettingsCard(title = stringResource(R.string.account)) {
            ListItem(
                modifier = Modifier.settingsCard(),
                headlineContent = {
                    HeadlineText(stringResource(R.string.link_account))
                },
                supportingContent = {
                    SupportingText(
                        stringResource(R.string.link_account_description)
                    )
                },
                trailingContent = {
                    Button(onClick = {
                        onSnackbar(featureNotImplementedString)
                    }) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(ButtonDefaults.IconSpacing),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                                painter = painterResource(id = R.drawable.discord),
                                contentDescription = stringResource(R.string.discord_icon)
                            )
                            Text(text = stringResource(R.string.connect))
                        }
                    }
                }
            )
        }

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
                        onCheckedChange = {
                            viewModel.allowExplicitContent(it)
                        }
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
                        onCheckedChange = {
                            viewModel.enableGameplayPreviewVideo(it)
                        }
                    )
                }
            )
        }

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
                        onCheckedChange = {
                            viewModel.useDynamicColors(it)
                        }
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
                        onThemeSelected = {
                            viewModel.updateAppTheme(it)
                        },
                        onCancel = {
                            viewModel.updateAppTheme(it)
                        }
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
                        /*ExposedDropdownMenuBoxUI(
                            options = listOf(
                                "Option 1",
                                "Option 2",
                                "Option 3",
                                "Option 1",
                                "Option 2",
                                "Option 3",
                                "Option 1",
                                "Option 2",
                                "Option 3",
                                "Option 1",
                                "Option 2",
                                "Option 3",
                                "Option 1",
                                "Option 2",
                                "Option 3",
                                "Option 1",
                                "Option 2",
                                "Option 3",
                            )
                        )*/
                    }
                },
                trailingContent = {
                    LanguageDialog()
                }
            )
        }

        SettingsCard(
            title = stringResource(R.string.version),
            supportingText = when (updateState) {
                is AppUpdateState.Downloading -> stringResource(
                    R.string.concluded_progress,
                    ((updateState as AppUpdateState.Downloading).progress * 100).toInt()
                )

                is AppUpdateState.UpdateAvailable -> stringResource(R.string.client_outdated)
                else -> null
            }
        ) {
            ListItem(
                modifier = Modifier.settingsCard(
                    padding = PaddingValues(top = 8.dp, bottom = 0.dp, start = 8.dp, end = 8.dp)
                ),
                headlineContent = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Outlined.Build, contentDescription = null)
                        HeadlineText(
                            when (updateState) {
                                is AppUpdateState.UpdateAvailable -> "$shrunkVersionName → $shrunkLatestVersion"
                                else -> shrunkVersionName
                            }
                        )
                    }
                },
                trailingContent = {
                    Button(
                        onClick = {
                            when (updateState) {
                                is AppUpdateState.UpdateAvailable -> {
                                    val updateInfo = updateState as AppUpdateState.UpdateAvailable
                                    scope.launch {
                                        viewModel.downloadUpdate(updateInfo.version)
                                    }
                                }

                                is AppUpdateState.ReadyToInstall -> {
                                    viewModel.installApk((updateState as AppUpdateState.ReadyToInstall).apkFile)
                                }

                                else -> {
                                    viewModel.checkAppUpdates()
                                }
                            }
                        },
                        enabled = updateState !is AppUpdateState.Checking &&
                                updateState !is AppUpdateState.Downloading
                    ) {
                        Text(
                            text = when (updateState) {
                                is AppUpdateState.Checking -> stringResource(R.string.checking_for_updates)
                                is AppUpdateState.UpdateAvailable -> stringResource(R.string.update_now)
                                is AppUpdateState.Downloading -> stringResource(R.string.downloading)
                                is AppUpdateState.ReadyToInstall -> stringResource(R.string.install_update)
                                else -> stringResource(R.string.check_for_updates)
                            }
                        )
                    }
                }
            )
            ListItem(
                modifier = Modifier.settingsCard(
                    padding = PaddingValues(top = 0.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
                ),
                headlineContent = {
                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth(),
                        onClick = {
                            LinkingUtils.openLink(
                                context,
                                "https://bscm.netlify.app/release-notes"
                            )
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = stringResource(R.string.read_changelog))
                            Icon(
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null
                            )
                        }
                    }
                },
            )
        }

        SettingsCard(title = stringResource(R.string.legal)) {
            ListItem(
                modifier = Modifier.settingsCard(
                    padding = PaddingValues(top = 8.dp, bottom = 0.dp, start = 8.dp, end = 8.dp)
                ),
                headlineContent = {
                    FilledTonalButton(
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        modifier = Modifier
                            .fillMaxWidth(),
                        onClick = {
                            LinkingUtils.openLink(
                                context,
                                "https://bscm.netlify.app/terms-of-service"
                            )
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = stringResource(R.string.terms_of_use))
                            Icon(
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null
                            )
                        }
                    }
                },
            )
            ListItem(
                modifier = Modifier.settingsCard(
                    padding = PaddingValues(top = 0.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
                ),
                headlineContent = {
                    FilledTonalButton(
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        modifier = Modifier
                            .fillMaxWidth(),
                        onClick = {
                            LinkingUtils.openLink(
                                context,
                                "https://bscm.netlify.app/privacy-policy"
                            )
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = stringResource(R.string.privacy_police))
                            Icon(
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null
                            )
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    supportingText: String? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
    ) {
        ListItem(
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.surfaceContainerHighest),
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            headlineContent = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
                )
            },
            trailingContent = {
                if (supportingText != null) {
                    Text(text = supportingText, style = MaterialTheme.typography.bodyMedium)
                }
            }
        )
        content()
    }
}

@Composable
private fun Modifier.settingsCard(padding: PaddingValues = PaddingValues(8.dp)): Modifier {
    return this.padding(padding)
}

@Composable
private fun ListDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier
            .height(1.dp)
            .fillMaxWidth()
    )
}

@Composable
private fun HeadlineText(title: String) {
    Text(
        text = title,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium
    )
}

@Composable
private fun SupportingText(title: String) {
    Text(text = title, style = MaterialTheme.typography.bodyMedium)
}