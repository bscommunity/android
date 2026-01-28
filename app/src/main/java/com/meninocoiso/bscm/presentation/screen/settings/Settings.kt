package com.meninocoiso.bscm.presentation.screen.settings

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.bscm.BuildConfig
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.model.User
import com.meninocoiso.bscm.presentation.navigation.OnSnackbar
import com.meninocoiso.bscm.presentation.navigation.show
import com.meninocoiso.bscm.presentation.ui.components.CollapsableSection
import com.meninocoiso.bscm.presentation.ui.components.SwitchUI
import com.meninocoiso.bscm.presentation.ui.components.dialog.ContributorsDialog
import com.meninocoiso.bscm.presentation.ui.components.dialog.LanguageDialog
import com.meninocoiso.bscm.presentation.ui.components.dialog.ThemeDialog
import com.meninocoiso.bscm.presentation.ui.components.layout.Avatar
import com.meninocoiso.bscm.presentation.ui.modifiers.fabScrollObserver
import com.meninocoiso.bscm.presentation.ui.modifiers.rememberFabNestedScrollConnection
import com.meninocoiso.bscm.presentation.ui.modifiers.roundedPolygonClip
import com.meninocoiso.bscm.presentation.ui.modifiers.roundedPolygonShape
import com.meninocoiso.bscm.presentation.viewmodel.AppUpdateState
import com.meninocoiso.bscm.presentation.viewmodel.AuthViewModel
import com.meninocoiso.bscm.presentation.viewmodel.SettingsViewModel
import com.meninocoiso.bscm.util.LinkingUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SettingsScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    startOAuth: (Uri) -> Unit,
    cacheUser: User?,
    onFabStateChange: (Boolean) -> Unit,
    onSnackbar: OnSnackbar,
    onNavigateToProfile: (user: User) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val contributorsState by viewModel.contributorsState.collectAsStateWithLifecycle()

    val activity = LocalActivity.current as ComponentActivity
    val authViewModel: AuthViewModel = hiltViewModel(activity)

    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    // Dialog state
    var showContributorsDialog by rememberSaveable { mutableStateOf(false) }

    // Seed cached user only once while restoring
    LaunchedEffect(cacheUser) {
        authViewModel.seedCachedUser(cacheUser)
    }

    // Defines which user to display 
    val displayUser = authState.user ?: cacheUser

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val shrunkVersionName = viewModel.shrunkVersion(BuildConfig.VERSION_NAME)
    val shrunkLatestVersion = if (updateState is AppUpdateState.UpdateAvailable)
        viewModel.shrunkVersion((updateState as AppUpdateState.UpdateAvailable).version)
    else ""

    // Collects Snackbar events from AuthViewModel
    LaunchedEffect(Unit) {
        authViewModel.snackbarEvents.collect { message ->
            onSnackbar.show(message)
        }
    }

    // Collects update events from SettingsViewModel
    LaunchedEffect(Unit) {
        viewModel.updateEvents.collect { message ->
            onSnackbar.show(message)
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
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.displayMedium
            )
        }

        // Account Section with Authentication
        SettingsCard(title = stringResource(R.string.account)) {
            if (authState.isLoggedIn && displayUser != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(vertical = 24.dp)
                    ) {
                        with(sharedTransitionScope) {
                            Avatar(
                                url = displayUser.imageUrl,
                                size = 128.dp,
                                modifier = Modifier
                                    .sharedElement(
                                        sharedTransitionScope.rememberSharedContentState(key = "profile_image"),
                                        animatedVisibilityScope = animatedContentScope
                                    )
                                    .border(
                                        width = 0.dp,
                                        color = MaterialTheme.colorScheme.outline,
                                        shape = roundedPolygonShape()
                                    )
                                    .roundedPolygonClip()
                                    .clickable(
                                        onClick = { onNavigateToProfile(displayUser) },
                                        indication = ripple(
                                            bounded = true,
                                            radius = Dp.Unspecified,
                                            color = Color.Black
                                        ),
                                        interactionSource = remember { MutableInteractionSource() }
                                    )
                            )
                            ProfileIndicator(
                                modifier = Modifier
                                    .sharedElement(
                                        sharedTransitionScope.rememberSharedContentState(key = "profile_icon"),
                                        animatedVisibilityScope = animatedContentScope
                                    )
                                    .graphicsLayer(
                                        alpha = 1f,
                                        scaleX = 1f,
                                        scaleY = 1f
                                    )
                            )
                        }

                    }
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
                                text = "Linked to @${displayUser.username}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        trailingContent = {
                            OutlinedButton(
                                onClick = {
                                    authViewModel.logout()
                                },
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
                            onClick = {
                                scope.launch {
                                    val uri = authViewModel.startDiscordOAuth()
                                    startOAuth(uri)
                                }
                            },
                            enabled = !authState.isLoading
                        ) {
                            if (authState.isLoading) {
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
                            viewModel.loadContributorsIfNeeded()
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
                                    .padding(vertical = 16.dp, horizontal = 24.dp)
                                ,
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
    }

    if (showContributorsDialog) {
        ContributorsDialog(
            isLoading = contributorsState.isLoading,
            items = contributorsState.items,
            onDismiss = { showContributorsDialog = false }
        )
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

@Composable
private fun SocialsRow(
    modifier: Modifier = Modifier,
    icon: Int,
    title: String,
    url: String,
) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = {
                    LinkingUtils.openLink(context, url)
                },
                indication = ripple(
                    bounded = true,
                    radius = Dp.Unspecified,
                    color = Color.Black
                ),
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
    }
}