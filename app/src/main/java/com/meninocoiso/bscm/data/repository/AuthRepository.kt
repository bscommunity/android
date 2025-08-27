package com.meninocoiso.bscm.data.repository

import android.util.Log
import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.data.security.SecureTokenManager
import com.meninocoiso.bscm.domain.model.User
import com.meninocoiso.bscm.domain.model.auth.AuthRequest
import com.meninocoiso.bscm.domain.model.auth.AuthResponse
import com.meninocoiso.bscm.domain.model.auth.RefreshTokenRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "AuthRepository"

@Singleton
class AuthRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val tokenManager: SecureTokenManager
) {
    
    suspend fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()
    fun isLoggedInFlow(): Flow<Boolean> = tokenManager.isLoggedInFlow()

    fun authenticateWithDiscord(code: String, redirectUri: String): Flow<Result<User>> = flow {
            Log.d(TAG, "authenticateWithDiscord: Starting authentication with code=${code.take(10)}..., redirectUri=$redirectUri")
            
            // Retrieve the stored code_verifier for PKCE
            val codeVerifier = tokenManager.getCodeVerifier()
            if (codeVerifier == null) {
                Log.e(TAG, "authenticateWithDiscord: Code verifier not found")
                emit(Result.failure(Exception("Code verifier não encontrado. Reinicie o fluxo de autenticação.")))
                return@flow
            }
            
        try {
            Log.d(TAG, "authenticateWithDiscord: Code verifier found, creating auth request")
            val authRequest = AuthRequest(code, redirectUri, codeVerifier)
            
            Log.d(TAG, "authenticateWithDiscord: Calling API client")
            val result = apiClient.authenticateWithDiscord(authRequest)
            
            Log.d(TAG, "authenticateWithDiscord: API call successful, saving tokens")
            tokenManager.saveTokens(result.accessToken, result.refreshToken)
            
            if (result.user == null) {
                Log.e(TAG, "authenticateWithDiscord: User data is null in the response")
                emit(Result.failure(Exception("User data is null in the response")))
                return@flow
            }
            
            Log.d(TAG, "authenticateWithDiscord: Authentication completed successfully")
            emit(Result.success(result.user))
        } catch (t: Throwable) {
            if (t is CancellationException) {
                Log.d(TAG, "authenticateWithDiscord: Flow cancelled")
                throw t // do not convert cancellation into failure
            }
            Log.e(TAG, "authenticateWithDiscord: Error occurred - ${t.message}", t)
            emit(Result.failure(t))
        } finally {
            // Always clear code_verifier after attempt
            tokenManager.clearCodeVerifier()
        }
    }

    fun refreshAccessToken(): Flow<Result<AuthResponse>> = flow {
            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken.isNullOrEmpty()) {
                emit(Result.failure(Exception("No refresh token available")))
                return@flow
            }

        try {
            val request = RefreshTokenRequest(refreshToken)
            val newTokens = apiClient.refreshToken(request)
            tokenManager.saveTokens(newTokens.accessToken, newTokens.refreshToken)
            emit(Result.success(newTokens))
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) {
                Log.d(TAG, "refreshAccessToken: Flow cancelled")
                throw t
            }
            // If the refresh token is invalid, clear the tokens
            if (t.message?.contains("invalid", ignoreCase = true) == true ||
                t.message?.contains("expired", ignoreCase = true) == true
            ) {
                tokenManager.clearTokens()
            }
            emit(Result.failure(t))
        }
    }

    fun getCurrentUser(): Flow<Result<User>> = flow {
        try {
            val user = apiClient.getCurrentUser()
            emit(Result.success(user))
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) {
                Log.d(TAG, "getCurrentUser: Flow cancelled")
                throw t
            }
            emit(Result.failure(t))
        }
    }

    suspend fun logout() {
        tokenManager.clearTokens()
    }

    suspend fun clearPkceVerifier() { tokenManager.clearCodeVerifier() }
}
