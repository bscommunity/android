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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
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

    /**
     * Restores the user session if possible.
     */
    fun seedCachedUser(user: User?) {
        if (user != null) {
            val current = _uiState.value
            if (current.user == null) {
                Log.d(TAG, "seedCachedUser: Seeding cached user: $user")
                _uiState.update { it.copy(user = user, isLoggedIn = true) }
            }
        }
    }

    /**
     * Call this when starting an OAuth flow to set state accordingly.
     */
    fun startPendingOAuth() {
        _uiState.update { it.copy(isLoading = true, error = null) }
    }

    /**
     * Call this when an OAuth flow is handled already (e.g. deep link succeeded).
     */
    fun completePendingOAuthHandled() {
        _uiState.update { it.copy(isLoading = false) }
    }
    
    /**
     * Starts the Discord OAuth flow
     */
    suspend fun startDiscordOAuth(): Uri {
        Log.d(TAG, "startDiscordOAuth: Starting OAuth process")
        return discordOAuth.getDiscordOAuthUri()
    }
    
    fun setError(message: String) {
        _uiState.update {
            it.copy(
                error = message,
                isLoading = false
            )
        }
    }

    /**
     * Cancels OAuth only if we're actually in the middle of one
     */
    fun cancelPendingOAuth() {
        Log.d(TAG, "cancelPendingOAuth: Cancelling pending OAuth")
        _uiState.update {
            it.copy(
                error = "Authentication cancelled",
                isLoading = false
            )
        }
    }

    fun handleAuthCallback(code: String) {
        if (code.isBlank()) {
            Log.e(TAG, "handleAuthCallback: Invalid authorization code")
            _uiState.update {
                it.copy(
                    error = "Invalid authorization code",
                    isLoading = false
                )
            }
            return
        }
        
        viewModelScope.launch {
            authRepository.authenticateWithDiscord(code, "bscm://auth")
                .catch { e ->
                    Log.e(TAG, "handleAuthCallback: Flow error - ${e.message}", e)
                    _uiState.update {
                        it.copy(
                            error = "Error during authentication: ${e.message}",
                            isLoading = false
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
                                    isLoading = false
                                )
                            }
                        },
                        onFailure = { ex ->
                            Log.e(TAG, "handleAuthCallback: Authentication failed - ${ex.message}", ex)
                            _uiState.update {
                                it.copy(
                                    error = "Authentication failed: ${ex.message}",
                                    isLoading = false
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
                    Log.d(TAG, "getCurrentUser: Fetching current user from API")
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
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { authRepository.logout() }
                .onSuccess {
                    Log.d(TAG, "logout: Logout successful")
                    _uiState.update { AuthUiState(isLoading = false) }
                }
                .onFailure { e ->
                    if (e is CancellationException) {
                        Log.d(TAG, "logout: Cancelled")
                        throw e
                    }
                    Log.e(TAG, "logout: Error during logout - ${e.message}", e)
                    _uiState.update { it.copy(error = "Error during logout: ${e.message}") }
                }
        }
    }
}