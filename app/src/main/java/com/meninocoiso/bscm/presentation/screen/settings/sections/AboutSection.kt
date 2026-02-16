package com.meninocoiso.bscm.presentation.screen.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.presentation.ui.components.CollapsableSection
import com.meninocoiso.bscm.presentation.ui.components.dialog.ContributorsDialog
import com.meninocoiso.bscm.presentation.ui.components.settings.SettingsCard
import com.meninocoiso.bscm.presentation.ui.components.settings.SocialsRow
import com.meninocoiso.bscm.presentation.ui.components.settings.settingsCard
import com.meninocoiso.bscm.presentation.viewmodel.SettingsViewModel

@Composable
fun AboutSection(
    contributorsState: SettingsViewModel.ContributorsState,
    loadContributorsIfNeeded: () -> Unit,
) {
    // Dialog state
    var showContributorsDialog by rememberSaveable { mutableStateOf(false) }

    SettingsCard(title = "About") {
        ListItem(
            modifier = Modifier.settingsCard(
                padding = PaddingValues(top = 8.dp, bottom = 0.dp, start = 8.dp, end = 8.dp)
            ),
            headlineContent = {
                FilledTonalButton(
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = {
                        showContributorsDialog = true
                        loadContributorsIfNeeded()
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Contributors")
                            Text(
                                text = "Check out the amazing people we have contributing to bscm",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
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
                CollapsableSection(
                    header = { trigger, interactionSource ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .indication(interactionSource, ripple())
                                .padding(vertical = 16.dp, horizontal = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Socials", style = MaterialTheme.typography.titleMedium)
                            trigger()
                        }
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow),
                    initExpanded = false
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SocialsRow(
                            icon = R.drawable.rounded_web_24,
                            title = "Website",
                            url = "https://bscm.netlify.app"
                        )
                        SocialsRow(
                            icon = R.drawable.discord,
                            title = "Discord",
                            url = "https://discord.gg/NNvzMAT6dS"
                        )
                        SocialsRow(
                            icon = R.drawable.github,
                            title = "GitHub",
                            url = "https://github.com/bscommunity"
                        )
                    }
                }
            },
        )
    }


    if (showContributorsDialog) {
        ContributorsDialog(
            isLoading = contributorsState.isLoading,
            items = contributorsState.items,
            onDismiss = { showContributorsDialog = false }
        )
    }
}