package com.meninocoiso.bscm.presentation.screen.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.BuildConfig
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.state.AppUpdateState
import com.meninocoiso.bscm.presentation.ui.components.settings.HeadlineText
import com.meninocoiso.bscm.presentation.ui.components.settings.SettingsCard
import com.meninocoiso.bscm.presentation.ui.components.settings.settingsCard
import com.meninocoiso.bscm.util.LinkingUtils
import java.io.File

@Composable
fun UpdateSection(
    updateState: AppUpdateState,
    downloadUpdate: (version: String) -> Unit,
    installApk: (apkFile: File) -> Unit,
    checkAppUpdates: () -> Unit,
) {
    val context = LocalContext.current

    val shrunkVersionName = shrunkVersion(BuildConfig.VERSION_NAME)
    val shrunkLatestVersion = if (updateState is AppUpdateState.UpdateAvailable)
        shrunkVersion(updateState.version)
    else "..."

    SettingsCard(
        title = stringResource(R.string.version),
        supportingText = when (updateState) {
            is AppUpdateState.Downloading -> stringResource(
                R.string.concluded_progress,
                (updateState.progress * 100).toInt()
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
                                downloadUpdate(updateState.version)
                            }

                            is AppUpdateState.ReadyToInstall -> {
                                installApk(updateState.apkFile)
                            }

                            else -> {
                                checkAppUpdates()
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
}

/** Helper to extract shrunk version name (removes suffix after last '-') */
private fun shrunkVersion(version: String): String = version.substringBeforeLast("-")