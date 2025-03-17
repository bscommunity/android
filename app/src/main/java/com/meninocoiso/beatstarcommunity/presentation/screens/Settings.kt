package com.meninocoiso.beatstarcommunity.presentation.screens

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.beatstarcommunity.BuildConfig
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.presentation.ui.components.SwitchUI
import com.meninocoiso.beatstarcommunity.presentation.ui.components.dialog.ThemeDialog
import com.meninocoiso.beatstarcommunity.presentation.ui.modifiers.fabScrollObserver
import com.meninocoiso.beatstarcommunity.presentation.ui.modifiers.rememberFabNestedScrollConnection
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.AppUpdateState
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.SettingsViewModel
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

    val scope = rememberCoroutineScope()

    LaunchedEffect(updateState) {
        when (updateState) {
            is AppUpdateState.UpToDate -> {
                onSnackbar("App is up to date")
            }

            is AppUpdateState.Error ->
                onSnackbar("Error checking for updates")

            else -> {}
        }
    }

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
            Text(text = "Settings", style = MaterialTheme.typography.displayMedium)
        }

        SettingsCard(title = "Account") {
            ListItem(
                modifier = Modifier.settingsCard(),
                headlineContent = {
                    HeadlineText("Link account")
                },
                supportingContent = {
                    SupportingText(
                        "Save your favorite content and keep it with yourself"
                    )
                },
                trailingContent = {
                    Button(onClick = {
                        onSnackbar("Account linking not yet implemented")
                    }) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(ButtonDefaults.IconSpacing),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                                painter = painterResource(id = R.drawable.discord),
                                contentDescription = "Discord icon"
                            )
                            Text(text = "Connect")
                        }
                    }
                }
            )
        }

        SettingsCard(title = "Preferences") {
            ListItem(
                modifier = Modifier.settingsCard(),
                headlineContent = {
                    HeadlineText("Explicit content")
                },
                supportingContent = {
                    SupportingText(
                        "Allow the display of charts with explicit songs"
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
            /*ListDivider()
            ListItem(
                modifier = Modifier.settingsCard(),
                headlineContent = {
                    HeadlineText("Displayed info")
                },
                supportingContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SupportingText(
                            "Choose the main information displayed on charts previews "
                        )
                        ExposedDropdownMenuBoxUI(
                            options = listOf(
                                "Option 1",
                                "Option 2",
                                "Option 3",
                            )
                        )
                    }
                }
            )*/
        }

        SettingsCard(title = "Customization") {
            ListItem(
                modifier = Modifier.settingsCard(),
                headlineContent = {
                    HeadlineText("Material You")
                },
                supportingContent = {
                    SupportingText(
                        "Toggle use of your system accent color"
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
                    HeadlineText("Theme")
                },
                supportingContent = {
                    SupportingText(
                        "Choose between your system preference, light and dark mode"
                    )
                },
                trailingContent = {
                    ThemeDialog(
                        option = uiState.theme,
                        onThemeSelected = {
                            viewModel.updateAppTheme(it)
                        }
                    )
                }
            )
        }

        SettingsCard(title = "Version") {
            ListItem(
                modifier = Modifier.settingsCard(
                    padding = PaddingValues(top = 8.dp, bottom = 0.dp, start = 8.dp, end = 8.dp)
                ),
                headlineContent = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Outlined.Build, contentDescription = "")
                        HeadlineText(
                            /*when(uiState.appUpdateVersion) {
                                "" -> BuildConfig.VERSION_NAME
                                else -> "${BuildConfig.VERSION_NAME} → ${uiState.appUpdateVersion}"
                            }*/
                            when (updateState) {
                                is AppUpdateState.UpdateAvailable -> "${BuildConfig.VERSION_NAME} → ${(updateState as AppUpdateState.UpdateAvailable).version}"
                                else -> BuildConfig.VERSION_NAME
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
                                is AppUpdateState.Checking -> "Checking for updates..."
                                is AppUpdateState.UpdateAvailable -> "Update now"
                                is AppUpdateState.Downloading -> "Downloading..."
                                else -> "Check for updates"
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
                        onClick = { /*TODO*/ }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Read changelog")
                            Icon(
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = ""
                            )
                        }
                    }
                },
            )
        }

        SettingsCard(title = "Legal") {
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
                        onClick = { /*TODO*/ }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Terms of Use")
                            Icon(
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = ""
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
                        onClick = { /*TODO*/ }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Disclaimers")
                            Icon(
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = ""
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
    Text(text = title, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun SupportingText(title: String) {
    Text(text = title, style = MaterialTheme.typography.bodyMedium)
}