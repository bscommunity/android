package com.meninocoiso.bscm

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.auth.AuthTabIntent
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var authTabLauncher: ActivityResultLauncher<Intent>
    private lateinit var fallbackLauncher: ActivityResultLauncher<Intent>

    private val viewModel: MainActivityViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    // Coordination state for a single in-flight OAuth flow
    // - oauthInProgress: whether we started a new OAuth flow and are still awaiting outcome
    // - oauthHandled: set to true when a success deep link has been handled (so we ignore later cancel)
    // - cancelDeferralJob: a short-lived job used to defer cancellation processing when result arrives first
    private var oauthInProgress = false
    private var oauthHandled = false
    private var cancelDeferralJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // 1) AuthTab launcher - official AuthTab callback registration
        // This launcher is used only when we choose to use AuthTab (see startOAuth).
        authTabLauncher = AuthTabIntent.registerActivityResultLauncher(this) { authResult ->
            // AuthTabIntent will call back with either a resultUri (success) or a null URI (canceled)
            // If AuthTab falls back to Custom Tab on this device, the callback will still be invoked
            // with resultUri == null on a user close.
            val resultUri = authResult?.resultUri
            if (resultUri != null) {
                // Auth success comes via callback
                handleAuthSuccess(resultUri)
            } else {
                // Auth result had no URI -> possible cancellation
                handlePotentialCancel()
            }
        }

        // 2) Fallback launcher for regular CustomTab startActivityForResult
        fallbackLauncher =
            registerForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
                ActivityResultCallback<ActivityResult> { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        // Some browsers might return OK but we still prefer deep link via onNewIntent.
                        // We don't rely on resultCode==OK for success; handle deep link in onNewIntent.
                        // For safety, treat only explicit URIs as success (we handle those separately).
                    } else {
                        // Usually RESULT_CANCELED => user closed Custom Tab.
                        handlePotentialCancel()
                    }
                })

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
                    // pass a lambda to start OAuth so Composables don't need to know launchers
                    startOAuth = { uri -> startOAuth(uri) }
                )

                NotificationsPermissionDialog()
            }
        }
    }

    /**
     * Called by SettingsScreen (or any UI) to start an OAuth flow for [uri].
     * This method chooses AuthTab when supported, otherwise uses a CustomTab fallback.
     */
    fun startOAuth(uri: Uri) {
        // mark we started a flow
        oauthInProgress = true
        oauthHandled = false
        cancelDeferralJob?.cancel()
        authViewModel.startPendingOAuth() // let ViewModel know an OAuth attempt started

        // Decide whether Auth Tab is supported: best-effort capability check:
        // Uses AndroidX Browser API that provides isAuthTabSupported(). If your
        // browser lib version does not provide it, fall back to trying AuthTab first.
        val supportsAuthTab = try {
            val provider = CustomTabsClient.getPackageName(this, null)
            provider != null && CustomTabsClient.isAuthTabSupported(this, provider)
        } catch (t: Throwable) {
            // defensive fallback if older library: assume not supported so we use Custom Tab fallback.
            false
        }

        if (supportsAuthTab) {
            // use AuthTabIntent: the registered authTabLauncher will receive the callback
            val authIntent = AuthTabIntent.Builder().build()
            authIntent.launch(authTabLauncher, uri, "bscm")
        } else {
            // fallback: launch a CustomTabsIntent via our fallback launcher so we receive a result
            // We choose the best package for Custom Tabs when possible:
            val packageName = CustomTabsClient.getPackageName(this, emptyList())
            val customTabsIntent = CustomTabsIntent.Builder().build()
            val intent = customTabsIntent.intent.apply {
                data = uri
                // If we found a suitable package, set it to prefer launching the CustomTabs-enabled browser
                if (packageName != null) setPackage(packageName)
            }
            fallbackLauncher.launch(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val data = intent.data
        if (data != null) {
            // A deep link arrived — treat as the authoritative success signal.
            // Clear any pending deferred cancellation and handle success.
            handleAuthSuccess(data)
        }
    }

    /**
     * When we get a URI (either from AuthTab callback or onNewIntent),
     * consider it the source of truth for success and ignore the later cancel close.
     */
    private fun handleAuthSuccess(uri: Uri) {
        cancelDeferralJob?.cancel()
        oauthHandled = true
        oauthInProgress = false

        // Extract code / error, route to current handling logic (keeps behaviour you already had)
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
                // No code/error in URI - treat as cancelled by provider
                println("OAuth callback without code or error")
                authViewModel.cancelPendingOAuth()
            }
        }
    }

    /**
     * Called when the tab close result (RESULT_CANCELED) or AuthTab callback with no URI is received.
     * We cannot immediately treat it as a real cancel because onNewIntent may still be delivered
     * right after. To solve the race safely we:
     *  - If there's no OAuth in progress -> ignore.
     *  - If OAuth is in progress but a deep link was already handled -> ignore.
     *  - Else: schedule a very short defer window (250ms) to allow the deep link to arrive,
     *    then actually process the cancel if nothing else happened.
     *
     * Note: This short deferral is the minimal, robust approach to resolve the platform ordering race.
     */
    private fun handlePotentialCancel() {
        if (!oauthInProgress) return
        if (oauthHandled) return

        // cancel previous deferral job if any, then start a new one
        cancelDeferralJob?.cancel()
        cancelDeferralJob = lifecycleScope.launch {
            // tiny grace period — small enough to be unnoticeable, large enough to let onNewIntent win
            delay(250L)
            // If still not handled -> this was a real cancel
            if (!oauthHandled) {
                oauthInProgress = false
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