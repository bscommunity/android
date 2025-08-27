package com.meninocoiso.bscm.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.bscm.data.repository.AuthRepository
import com.meninocoiso.bscm.data.security.DiscordOAuth
import com.meninocoiso.bscm.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val user: User? = null,
    val error: String? = null
)

private const val TAG = "AuthViewModel"

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val discordOAuth: DiscordOAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Controle de fluxo OAuth
    private var waitingForCallback: Boolean = false
    private var oauthStartTime: Long = 0L
    private var oauthTimeoutJob = null as kotlinx.coroutines.Job?
    private companion object {
        const val OAUTH_CANCEL_GRACE_MS = 0L
        const val OAUTH_MAX_WAIT_MS = 1_000L
    }

    init {
        checkAuthState()
        observeLoginState()
    }

    private fun observeLoginState() {
        viewModelScope.launch {
            authRepository.isLoggedInFlow()
                .distinctUntilChanged()
                .collect { logged ->
                    val previous = _uiState.value.isLoggedIn
                    if (logged != previous) {
                        _uiState.value = _uiState.value.copy(isLoggedIn = logged)
                        if (logged) {
                            getCurrentUser()
                        } else {
                            _uiState.value = AuthUiState()
                        }
                    }
                }
        }
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            val isLoggedIn = authRepository.isLoggedIn()
            _uiState.value = _uiState.value.copy(isLoggedIn = isLoggedIn)

            if (isLoggedIn) {
                getCurrentUser()
            }
        }
    }

    fun startDiscordOAuth(context: Context) {
        viewModelScope.launch {
            // If there was already a pending flow, cancel it
            if (waitingForCallback) {
                cancelPendingOAuth("Restarting authentication flow")
            }
            waitingForCallback = true
            oauthStartTime = System.currentTimeMillis()

            // Set loading and clear previous error before starting OAuth
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            // Use runCatching so we do not accidentally swallow CancellationException
            runCatching {
                discordOAuth.startDiscordOAuth(context) // may throw if intent/custom tab cannot be launched
                scheduleOAuthTimeout()
            }.onSuccess {
                // Keep isLoading = true; the flow will effectively continue when handleAuthCallback() is invoked
            }.onFailure { e ->
                waitingForCallback = false
                oauthTimeoutJob?.cancel()
                if (e is CancellationException) {
                    // Propagate cancellation and update state accordingly
                    Log.d(TAG, "startDiscordOAuth: Authentication cancelled")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Authentication cancelled"
                    )
                    throw e
                }
                Log.e(TAG, "startDiscordOAuth: Error - ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error starting authentication: ${e.message}"
                )
            }
        }
    }

    private fun scheduleOAuthTimeout() {
        oauthTimeoutJob?.cancel()
        oauthTimeoutJob = viewModelScope.launch {
            delay(OAUTH_MAX_WAIT_MS)
            if (waitingForCallback) {
                cancelPendingOAuth("Timeout de autenticação")
            }
        }
    }

    fun onAppResumed() {
        if (waitingForCallback) {
            // No grace period: any return while waiting means cancellation
            cancelPendingOAuth("Custom Tab closed or user returned without completing")
        }
    }

    private fun cancelPendingOAuth(reason: String) {
        waitingForCallback = false
        oauthTimeoutJob?.cancel()
        // Removed: Do not clear code_verifier on cancellation
        // viewModelScope.launch { authRepository.clearPkceVerifier() }
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = "Authentication cancelled"
        )
        Log.d(TAG, "OAuth cancelled: $reason")
    }

    fun handleAuthCallback(code: String) {
        waitingForCallback = false
        oauthTimeoutJob?.cancel()
        if (code.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Invalid authorization code"
            )
            return
        }
        viewModelScope.launch {
            authRepository.authenticateWithDiscord(code, "bscm://auth")
                .onStart {
                    if (!_uiState.value.isLoading) {
                        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                    }
                }
                .catch { e ->
                    if (e is CancellationException) {
                        Log.d(TAG, "handleAuthCallback: Authentication cancelled")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Authentication cancelled"
                        )
                        throw e
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Error during authentication: ${e.message}"
                        )
                    }
                }
                .onCompletion { cause ->
                    if (cause == null && _uiState.value.isLoading) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { user ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isLoggedIn = true, // redundante mas garante estado imediato até o flow propagar
                                user = user,
                                error = null
                            )
                        },
                        onFailure = { ex ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Authentication failed: ${ex.message}"
                            )
                        }
                    )
                }
        }
    }

    private fun getCurrentUser() {
        viewModelScope.launch {
            authRepository.getCurrentUser()
                .onStart {
                    Log.d(TAG, "getCurrentUser: Fetching current user")
                }
                .catch { e ->
                    Log.e(TAG, "getCurrentUser: Flow exception, logging out - ${e.message}", e)
                    // If it fails to get the user, the token may be invalid
                    logout()
                }
                .collect { result ->
                    result
                        .onSuccess { user ->
                            Log.d(TAG, "getCurrentUser: User fetched = $user")
                            _uiState.value = _uiState.value.copy(user = user)
                        }
                        .onFailure { ex ->
                            Log.e(
                                TAG,
                                "getCurrentUser: Failed to fetch user (${ex.message}), logging out"
                            )
                            // If it fails to get the user, the token may be invalid
                            logout()
                        }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
                Log.d(TAG, "logout: Logging out")
            // Use runCatching for consistent error handling and to avoid swallowing CancellationException
            runCatching {
                authRepository.logout() // suspend call (if turned into a Flow later, adapt with onStart/catch/onCompletion)
            }.onSuccess {
                // Reset to initial state after successful logout
                _uiState.value = AuthUiState()
            }.onFailure { e ->
                if (e is CancellationException) {
                    Log.d(TAG, "logout: Cancelled")
                    throw e // propagate cancellation
                }
                Log.e(TAG, "logout: Error during logout - ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = "Error during logout: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        Log.d(TAG, "clearError: Clearing error")
        _uiState.value = _uiState.value.copy(error = null)
    }
}