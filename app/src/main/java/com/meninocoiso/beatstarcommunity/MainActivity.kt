package com.meninocoiso.beatstarcommunity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.meninocoiso.beatstarcommunity.domain.enums.ThemePreference
import com.meninocoiso.beatstarcommunity.presentation.navigation.MainNav
import com.meninocoiso.beatstarcommunity.presentation.ui.components.dialog.NotificationsPermissionDialog
import com.meninocoiso.beatstarcommunity.presentation.ui.theme.BeatstarCommunityTheme
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.MainActivityUiState
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.MainActivityUiState.Loading
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.MainActivityUiState.Success
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.MainActivityViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        var uiState: MainActivityUiState by mutableStateOf(Loading)

        // Update the uiState
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState
                    .onEach { uiState = it }
                    .collect(
                        collector = ::println
                    )
            }
        }

        // Cleanup old updates
        viewModel.cleanupOldUpdates()

        // Keep the splash screen on-screen until the UI cacheState is loaded. 
        // This condition is evaluated each time the app needs to be redrawn 
        // so it should be fast to avoid blocking the UI.
        splashScreen.setKeepOnScreenCondition {
            when (uiState) {
                Loading -> true
                is Success -> false
            }
        }

        // Customize exit animation (fade out)
        splashScreen.setOnExitAnimationListener { splashView ->
            splashView.view.animate()
                .alpha(0f)
                .setDuration(175L) // Fade out duration
                .withEndAction {
                    splashView.remove()
                }
                .start()
        }
        
        setContent {
            val darkTheme =
                shouldUseDarkTheme(uiState)

            // Turn off the decor fitting system windows, which allows us to handle insets,
            // including IME animations, and go edge-to-edge
            // This also sets up the initial system bar style based on the platform theme
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        lightScrim = android.graphics.Color.TRANSPARENT,
                        darkScrim = android.graphics.Color.TRANSPARENT,
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.light(
                        android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
                    )
                )
                onDispose {}
            }

            BeatstarCommunityTheme(
                darkTheme = darkTheme,
                dynamicColor = shouldUseDynamicTheming(uiState),
            ) {
                MainNav(
                    hasUpdate = when (uiState) {
                        Loading -> false
                        is Success -> viewModel.hasUpdate((uiState as Success).settings.latestUpdateVersion)
                    }
                )

                NotificationsPermissionDialog()
            }
        }
    }
}

/**
 * Returns `true` if the dynamic color is enabled, as a function of the [uiState].
 */
@Composable
private fun shouldUseDynamicTheming(
    uiState: MainActivityUiState,
): Boolean = when (uiState) {
    Loading -> true
    is Success -> uiState.settings.useDynamicColors
}

/**
 * Returns `true` if dark theme should be used, as a function of the [uiState] and the
 * current system context.
 */
@Composable
private fun shouldUseDarkTheme(
    uiState: MainActivityUiState,
): Boolean = when (uiState) {
    Loading -> isSystemInDarkTheme()
    is Success -> when (uiState.settings.theme) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
}