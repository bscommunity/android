package com.meninocoiso.bscm.presentation.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.repository.AuthRepository
import com.meninocoiso.bscm.data.security.DiscordOAuth
import com.meninocoiso.bscm.domain.result.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import java.security.SecureRandom
import javax.inject.Inject

private const val TAG = "AuthViewModel"

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val discordOAuth: DiscordOAuth,
) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    val isLoggedInFlow = authRepository.isLoggedInFlow()

    suspend fun getCurrentUserId(): String? {
        return authRepository.getCurrentUserId()
    }

    private val _snackbarEvents = MutableSharedFlow<UiText>()
    val snackbarEvents: SharedFlow<UiText> = _snackbarEvents.shareIn(
        viewModelScope,
        SharingStarted.Lazily,
        replay = 0
    )

    // If we ever need to handle errors in other ways (e.g., dialogs, logging, analytics), 
    // keep error-related events separate, in something like errorEvents.

    /**
     * Call this when starting an OAuth flow to set state accordingly.
     */
    fun startPendingOAuth() {
        _isLoading.value = true
    }

    /**
     * Starts the Discord OAuth flow
     */
    suspend fun getAuthorizationUrl(): Uri {
        Log.d(TAG, "startDiscordOAuth: Starting OAuth process")
        // generate a random state token and persist it to validate redirects
        val state = generateStateToken()
        authRepository.setPendingOAuthState(state)
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
            _isLoading.value = false
            _snackbarEvents.emit(UiText.Plain(message))
        }
    }

    /**
     * Cancels OAuth only if we're actually in the middle of one
     */
    fun cancelPendingOAuth() {
        Log.d(TAG, "cancelPendingOAuth: Cancelling pending OAuth")
        viewModelScope.launch {
            _snackbarEvents.emit(UiText.Res(R.string.auth_cancelled))
        }
        _isLoading.value = false
        // clear any persisted pending state
        viewModelScope.launch {
            authRepository.clearPendingOAuthState()
        }
    }

    fun handleAuthCallback(code: String) {
        if (code.isBlank()) {
            Log.e(TAG, "handleAuthCallback: Invalid authorization code")
            viewModelScope.launch {
                _snackbarEvents.emit(UiText.Res(R.string.error_invalid_auth_code))
            }
            _isLoading.value = false
            return
        }
        viewModelScope.launch {
            authRepository.authenticateWithDiscord(code, "bscm://auth/callback")
                .catch { e ->
                    Log.e(TAG, "handleAuthCallback: Flow error - ${e.message}", e)
                    _snackbarEvents.emit(UiText.Res(R.string.error_login, e.message ?: ""))
                    _isLoading.value = false
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { user ->
                            Log.d(TAG, "handleAuthCallback: Authentication successful")
                            _isLoading.value = false

                            _snackbarEvents.emit(
                                UiText.Res(
                                    R.string.login_success,
                                    user.username
                                )
                            )

                            // clear persisted pending state on success
                            authRepository.clearPendingOAuthState()
                        },
                        onFailure = { ex ->
                            Log.e(
                                TAG,
                                "handleAuthCallback: Authentication failed - ${ex.message}",
                                ex
                            )
                            _snackbarEvents.emit(
                                UiText.Res(
                                    R.string.error_login,
                                    ex.message ?: ""
                                )
                            )
                            _isLoading.value = false
                            // clear persisted pending state on failure as well
                            authRepository.clearPendingOAuthState()
                        }
                    )
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            Log.d(TAG, "logout: Logging out")
            _isLoading.value = true
            runCatching { authRepository.logout() }
                .onSuccess {
                    Log.d(TAG, "logout: Logout successful")
                    _isLoading.value = false
                    _snackbarEvents.emit(UiText.Res(R.string.logout_success))
                }
                .onFailure { e ->
                    if (e is CancellationException) {
                        Log.d(TAG, "logout: Cancelled")
                        throw e
                    }
                    Log.e(TAG, "logout: Error during logout - ${e.message}", e)
                    _snackbarEvents.emit(UiText.Res(R.string.error_logout, e.message ?: ""))
                    _isLoading.value = false
                }
        }
    }
}
