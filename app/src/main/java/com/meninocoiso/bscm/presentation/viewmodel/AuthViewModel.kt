package com.meninocoiso.bscm.presentation.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.repository.AuthRepository
import com.meninocoiso.bscm.data.repository.CacheRepository
import com.meninocoiso.bscm.data.security.DiscordOAuth
import com.meninocoiso.bscm.domain.model.User
import com.meninocoiso.bscm.domain.state.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.SecureRandom
import javax.inject.Inject

private const val TAG = "AuthViewModel"

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val discordOAuth: DiscordOAuth,
    private val cacheRepository: CacheRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthState())
    val uiState: StateFlow<AuthState> = _uiState.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>()
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.shareIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, replay = 0)

    // If we ever need to handle errors in other ways (e.g., dialogs, logging, analytics), 
    // keep error-related events separate, in something like errorEvents.
    
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
        _uiState.update { it.copy(isLoading = true) }
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
        // generate a random state token and persist it to validate redirects
        val state = generateStateToken()
        cacheRepository.setPendingOAuthState(state)
        return discordOAuth.getDiscordOAuthUri(state)
    }

    private fun generateStateToken(): String {
        val secureRandom = SecureRandom()
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    fun setError(message: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = false) }
            _snackbarEvents.emit(message)
        }
    }

    /**
     * Cancels OAuth only if we're actually in the middle of one
     */
    fun cancelPendingOAuth() {
        Log.d(TAG, "cancelPendingOAuth: Cancelling pending OAuth")
        viewModelScope.launch {
            _snackbarEvents.emit(context.getString(R.string.auth_cancelled))
        }
        _uiState.update {
            it.copy(isLoading = false)
        }
        // clear any persisted pending state
        viewModelScope.launch {
            cacheRepository.clearPendingOAuthState()
        }
    }

    fun handleAuthCallback(code: String) {
        if (code.isBlank()) {
            Log.e(TAG, "handleAuthCallback: Invalid authorization code")
            viewModelScope.launch {
                _snackbarEvents.emit(context.getString(R.string.error_invalid_auth_code))
            }
            _uiState.update {
                it.copy(isLoading = false)
            }
            return
        }
        viewModelScope.launch {
            authRepository.authenticateWithDiscord(code, "bscm://auth/callback")
                .catch { e ->
                    Log.e(TAG, "handleAuthCallback: Flow error - ${e.message}", e)
                    _snackbarEvents.emit(context.getString(R.string.error_login, e.message ?: ""))
                    _uiState.update {
                        it.copy(isLoading = false)
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
                                    isLoading = false
                                )
                            }
                            _snackbarEvents.emit(context.getString(R.string.login_success, user.username))
                            // clear persisted pending state on success
                            cacheRepository.clearPendingOAuthState()
                        },
                        onFailure = { ex ->
                            Log.e(TAG, "handleAuthCallback: Authentication failed - ${ex.message}", ex)
                            _snackbarEvents.emit(context.getString(R.string.error_login, ex.message ?: ""))
                            _uiState.update {
                                it.copy(isLoading = false)
                            }
                            // clear persisted pending state on failure as well
                            cacheRepository.clearPendingOAuthState()
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
                isLoading = false
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            Log.d(TAG, "logout: Logging out")
            _uiState.update { it.copy(isLoading = true) }
            runCatching { authRepository.logout() }
                .onSuccess {
                    Log.d(TAG, "logout: Logout successful")
                    _uiState.update { AuthState(isLoading = false) }
                    _snackbarEvents.emit(context.getString(R.string.logout_success))
                }
                .onFailure { e ->
                    if (e is CancellationException) {
                        Log.d(TAG, "logout: Cancelled")
                        throw e
                    }
                    Log.e(TAG, "logout: Error during logout - ${e.message}", e)
                    _snackbarEvents.emit(context.getString(R.string.error_logout, e.message ?: ""))
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }
}