package com.meninocoiso.bscm.presentation.viewmodel

import android.net.Uri
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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Represents the different states of the OAuth authentication process
 * Think of this like a traffic light system - each state tells us exactly where we are
 */
enum class OAuthState {
    IDLE,           // No OAuth process running
    IN_PROGRESS,    // OAuth browser tab is open, waiting for user action
    PROCESSING,     // User completed OAuth, we're processing the callback (red light - busy)
}

data class AuthUiState(
    val oAuthState: OAuthState = OAuthState.IDLE,
    val isLoggedIn: Boolean = false,
    val user: User? = null,
    val error: String? = null,
)

private const val TAG = "AuthViewModel"

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val discordOAuth: DiscordOAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

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
                        _uiState.update { it.copy(isLoggedIn = logged) }
                        if (logged) {
                            getCurrentUser()
                        } else {
                            clearUserData()
                        }
                    }
                }
        }
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            val isLoggedIn = authRepository.isLoggedIn()
            _uiState.update { it.copy(isLoggedIn = isLoggedIn) }
            if (isLoggedIn) {
                getCurrentUser()
            }
        }
    }

    /**
     * Starts the Discord OAuth flow
     */
    suspend fun startDiscordOAuth(): Uri {
        Log.d(TAG, "startDiscordOAuth: Starting OAuth process")
        _uiState.update {
            it.copy(
                error = null,
                oAuthState = OAuthState.IN_PROGRESS
            )
        }
        return discordOAuth.discordOAuthIntent()
    }
    
    fun setError(message: String) {
        _uiState.update {
            it.copy(
                error = message,
                oAuthState = OAuthState.IDLE
            )
        }
    }

    /**
     * Cancels OAuth only if we're actually in the middle of one
     */
    fun cancelPendingOAuth() {
        viewModelScope.launch {
            delay(500)
            val currentState = _uiState.value
            
            Log.d(TAG, "cancelPendingOAuth: Current OAuth state = ${currentState.oAuthState}")
            
            if (currentState.oAuthState == OAuthState.IN_PROGRESS) {
                _uiState.update {
                    it.copy(
                        error = "Authentication cancelled",
                        oAuthState = OAuthState.IDLE
                    )
                }
            }
        }
    }

    /**
     * Handles the OAuth callback from Discord
     * Think of this like receiving a package - we first check if we were expecting it,
     * then process it, then update our records
     */
    fun handleAuthCallback(code: String) {
        if (code.isBlank()) {
            Log.e(TAG, "handleAuthCallback: Invalid authorization code")
            _uiState.update {
                it.copy(
                    error = "Invalid authorization code",
                    oAuthState = OAuthState.IDLE
                )
            }
            return
        }
        
        // Update state to processing
        _uiState.update {
            it.copy(
                error = null,
                oAuthState = OAuthState.PROCESSING
            )
        }


        Log.d(TAG, "handleAuthCallback: Received callback, current state: ${_uiState.value.oAuthState}")
        
        viewModelScope.launch {
            authRepository.authenticateWithDiscord(code, "bscm://auth")
                .catch { e ->
                    Log.e(TAG, "handleAuthCallback: Flow error - ${e.message}", e)
                    _uiState.update {
                        it.copy(
                            error = "Error during authentication: ${e.message}",
                            oAuthState = OAuthState.IDLE
                        )
                    }
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { user ->
                            Log.d(TAG, "handleAuthCallback: Authentication successful")
                            _uiState.update {
                                it.copy(
                                    isLoggedIn = true,
                                    user = user,
                                    error = null,
                                    oAuthState = OAuthState.IDLE
                                )
                            }
                        },
                        onFailure = { ex ->
                            Log.e(TAG, "handleAuthCallback: Authentication failed - ${ex.message}", ex)
                            _uiState.update {
                                it.copy(
                                    error = "Authentication failed: ${ex.message}",
                                    oAuthState = OAuthState.IDLE
                                )
                            }
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
                    logout()
                }
                .collect { result ->
                    result
                        .onSuccess { user ->
                            Log.d(TAG, "getCurrentUser: User fetched = $user")
                            _uiState.update { it.copy(user = user) }
                        }
                        .onFailure { ex ->
                            Log.e(
                                TAG,
                                "getCurrentUser: Failed to fetch user (${ex.message}), logging out"
                            )
                            logout()
                        }
                }
        }
    }

    /**
     * Clears all user-related data from the UI state
     * Think of this like cleaning a slate - we keep the structure but remove the content
     */
    private fun clearUserData() {
        _uiState.update {
            it.copy(
                user = null,
                isLoggedIn = false,
                error = null,
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            Log.d(TAG, "logout: Logging out")

            // Show loading state during logout
            _uiState.update { it.copy(oAuthState = OAuthState.PROCESSING, error = null) }

            runCatching {
                authRepository.logout()
            }.onSuccess {
                Log.d(TAG, "logout: Logout successful")
                _uiState.update {
                    AuthUiState() // Reset to clean initial state
                }
            }.onFailure { e ->
                if (e is CancellationException) {
                    Log.d(TAG, "logout: Cancelled")
                    throw e
                }
                Log.e(TAG, "logout: Error during logout - ${e.message}", e)
                _uiState.update {
                    it.copy(error = "Error during logout: ${e.message}")
                }
            }
        }
    }
}