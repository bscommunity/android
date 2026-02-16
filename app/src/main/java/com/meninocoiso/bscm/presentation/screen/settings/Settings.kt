package com.meninocoiso.bscm.presentation.screen.settings

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.remote.dto.user.SimplifiedUser
import com.meninocoiso.bscm.domain.model.User
import com.meninocoiso.bscm.presentation.navigation.OnSnackbar
import com.meninocoiso.bscm.presentation.navigation.show
import com.meninocoiso.bscm.presentation.screen.settings.sections.AboutSection
import com.meninocoiso.bscm.presentation.screen.settings.sections.AccountSection
import com.meninocoiso.bscm.presentation.screen.settings.sections.CustomizationSection
import com.meninocoiso.bscm.presentation.screen.settings.sections.PreferencesSection
import com.meninocoiso.bscm.presentation.screen.settings.sections.UpdateSection
import com.meninocoiso.bscm.presentation.ui.modifiers.fabScrollObserver
import com.meninocoiso.bscm.presentation.ui.modifiers.rememberFabNestedScrollConnection
import com.meninocoiso.bscm.presentation.viewmodel.AuthViewModel
import com.meninocoiso.bscm.presentation.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    user: User?,
    startOAuth: (Uri) -> Unit,
    onFabStateChange: (Boolean) -> Unit,
    onSnackbar: OnSnackbar,
    onNavigateToProfile: (user: SimplifiedUser, isOwner: Boolean, isFollowing: Boolean) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val contributorsState by viewModel.contributorsState.collectAsStateWithLifecycle()

    val activity = LocalActivity.current as ComponentActivity
    val authViewModel: AuthViewModel = hiltViewModel(activity)

    val scope = rememberCoroutineScope()
    val isLoading by authViewModel.isLoading.collectAsStateWithLifecycle()

    // Collects auth events from AuthViewModel
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
        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp)) {
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.displayMedium
            )
        }

        // Account Section with Authentication
        AccountSection(
            user = user,
            isLoading = isLoading,
            login = { scope.launch { startOAuth(authViewModel.getAuthorizationUrl()) } },
            logout = { authViewModel.logout() },
            onNavigateToProfile = onNavigateToProfile,
            sharedTransitionScope = sharedTransitionScope,
            animatedContentScope = animatedContentScope,
        )

        PreferencesSection(
            uiState = uiState,
            allowExplicitContent = { viewModel.allowExplicitContent(it) },
            enableGameplayPreviewVideo = { viewModel.enableGameplayPreviewVideo(it) }
        )

        CustomizationSection(
            uiState = uiState,
            useDynamicColors = { viewModel.useDynamicColors(it) },
            updateAppTheme = { viewModel.updateAppTheme(it) }
        )

        UpdateSection(
            updateState = updateState,
            checkAppUpdates = { viewModel.checkAppUpdates() },
            downloadUpdate = { viewModel.downloadUpdate(it) },
            installApk = { viewModel.installApk(it) }
        )

        AboutSection(
            contributorsState = contributorsState,
            loadContributorsIfNeeded = { viewModel.loadContributorsIfNeeded() }
        )
    }
}