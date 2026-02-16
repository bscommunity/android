package com.meninocoiso.bscm.presentation.screen.settings.sections

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.model.User
import com.meninocoiso.bscm.presentation.screen.profile.OnNavigateToProfile
import com.meninocoiso.bscm.presentation.ui.components.SwitchUI
import com.meninocoiso.bscm.presentation.ui.components.settings.AnimatedAccountHeader
import com.meninocoiso.bscm.presentation.ui.components.settings.HeadlineText
import com.meninocoiso.bscm.presentation.ui.components.settings.ListDivider
import com.meninocoiso.bscm.presentation.ui.components.settings.SettingsCard
import com.meninocoiso.bscm.presentation.ui.components.settings.SupportingText
import com.meninocoiso.bscm.presentation.ui.components.settings.settingsCard

@Composable
fun AccountSection(
    user: User?,
    isLoading: Boolean,
    login: () -> Unit,
    logout: () -> Unit,
    onNavigateToProfile: OnNavigateToProfile,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
) {
    SettingsCard(title = stringResource(R.string.account)) {
        if (user != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedAccountHeader(
                    user = user,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedContentScope = animatedContentScope,
                    onNavigateToProfile = onNavigateToProfile
                )
                ListItem(
                    modifier = Modifier.settingsCard(
                        padding = PaddingValues(
                            horizontal = 8.dp,
                            vertical = 2.dp
                        )
                    ),
                    headlineContent = {
                        HeadlineText("Public profile")
                    },
                    supportingContent = {
                        SupportingText("Other users can see your liked content")
                    },
                    trailingContent = {
                        SwitchUI(
                            checked = false,
                            onCheckedChange = {},
                            enabled = false
                        )
                    }
                )
                ListDivider()
                ListItem(
                    modifier = Modifier.settingsCard(),
                    headlineContent = {
                        Text(
                            text = "Linked to @${user.username}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    trailingContent = {
                        OutlinedButton(
                            onClick = logout,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Unlink account")
                        }
                    }
                )
            }
        } else {
            // User is not logged in - show login option
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
                    Button(
                        onClick = login,
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                                strokeWidth = 2.dp
                            )
                        } else {
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
                }
            )
        }
    }
}