package com.meninocoiso.bscm.data.security

import android.util.Log
import com.meninocoiso.bscm.data.manager.SecureTokenManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AuthInterceptor"

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: SecureTokenManager
) {
    private val refreshMutex = Mutex()
    private var tokenRefreshCallback: (suspend () -> Boolean)? = null
    @Volatile
    private var isCurrentlyRefreshing = false

    fun setTokenRefreshCallback(callback: suspend () -> Boolean) {
        tokenRefreshCallback = callback
    }

    suspend fun getAuthToken(): String? {
        // Simply return the current token without blocking
        // If it's expired, the 401 response handler will trigger refresh
        return tokenManager.getAccessToken()
    }

    /**
     * Attempts to refresh the token if it's expired.
     * Returns true if refresh was successful or not needed, false otherwise.
     * This should be called from a coroutine context (not blocking).
     */
    suspend fun refreshTokenIfNeeded(): Boolean {
        // Check if token is expired or about to expire
        if (!tokenManager.isTokenExpired()) {
            return true // Token is still valid
        }

        Log.d(TAG, "Token is expired or about to expire, attempting refresh")
        return refreshInternal(allowSkipWhenValid = true)
    }

    /**
     * Forces a token refresh regardless of expiry state (e.g. after a 401).
     * Returns true if refresh was successful, false otherwise.
     */
    suspend fun forceRefreshToken(): Boolean {
        Log.d(TAG, "Forcing token refresh after 401")
        return refreshInternal(allowSkipWhenValid = false)
    }

    private suspend fun refreshInternal(allowSkipWhenValid: Boolean): Boolean {
        return refreshMutex.withLock {
            if (allowSkipWhenValid && !tokenManager.isTokenExpired()) {
                Log.d(TAG, "Token was already refreshed by another coroutine")
                return@withLock true
            }

            if (isCurrentlyRefreshing) {
                Log.d(TAG, "Token refresh already in progress")
                return@withLock false
            }

            isCurrentlyRefreshing = true
            try {
                val refreshed = tokenRefreshCallback?.invoke() ?: false
                if (!refreshed) {
                    Log.e(TAG, "Token refresh failed")
                    return@withLock false
                }
                Log.d(TAG, "Token refreshed successfully")
                true
            } finally {
                isCurrentlyRefreshing = false
            }
        }
    }
}
