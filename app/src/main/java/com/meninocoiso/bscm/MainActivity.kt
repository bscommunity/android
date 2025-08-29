package com.meninocoiso.bscm

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.auth.AuthTabIntent
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
import com.meninocoiso.bscm.domain.enums.ThemePreference
import com.meninocoiso.bscm.presentation.navigation.MainNav
import com.meninocoiso.bscm.presentation.ui.components.dialog.NotificationsPermissionDialog
import com.meninocoiso.bscm.presentation.ui.theme.BeatstarCommunityTheme
import com.meninocoiso.bscm.presentation.viewmodel.AuthViewModel
import com.meninocoiso.bscm.presentation.viewmodel.MainActivityUiState
import com.meninocoiso.bscm.presentation.viewmodel.MainActivityUiState.Loading
import com.meninocoiso.bscm.presentation.viewmodel.MainActivityUiState.Success
import com.meninocoiso.bscm.presentation.viewmodel.MainActivityViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var authTabLauncher: ActivityResultLauncher<Intent>
    
    private val viewModel: MainActivityViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Handle OAuth intent if the activity was launched with one
        authTabLauncher = AuthTabIntent.registerActivityResultLauncher(this) { authResult ->
            println("AuthTab result: code=${authResult?.resultCode}, uri=${authResult?.resultUri}")

            // If no URI: treat as cancellation
            if (authResult?.resultUri == null) {
                println("AuthTab closed without URI -> treat as cancellation")
                processOAuthResult(null)
                return@registerActivityResultLauncher
            }

            // If URI present: process it
            processOAuthResult(authResult.resultUri)
        }


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
                        is Success -> viewModel.hasUpdate((uiState as Success).latestUpdateVersion)
                    },
                    authTabLauncher = authTabLauncher,
                )

                NotificationsPermissionDialog()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        println("Deep link received, handling via deep link: ${intent.data}")
        processOAuthResult(intent.data)
    }

    private fun processOAuthResult(uri: Uri?) {
        if (uri == null) {
            println("OAuth cancelled or closed without URI")
            authViewModel.cancelPendingOAuth()
            return
        }

        val code = uri.getQueryParameter("code")
        val error = uri.getQueryParameter("error")

        when {
            code != null -> {
                println("OAuth completed with code: $code")
                authViewModel.handleAuthCallback(code)
            }
            error != null -> {
                println("OAuth error: $error")
                authViewModel.setError(error)
            }
            else -> {
                println("OAuth cancelled (no code/error in URI)")
                authViewModel.cancelPendingOAuth()
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