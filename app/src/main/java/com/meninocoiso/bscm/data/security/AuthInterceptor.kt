package com.meninocoiso.bscm.data.security

import android.util.Log
import com.meninocoiso.bscm.data.manager.SecureTokenManager
import com.meninocoiso.bscm.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AuthInterceptor"

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: SecureTokenManager,
    @param:ApplicationScope private val coroutineScope: CoroutineScope
) {
    private val refreshMutex = Mutex()
    private var tokenRefreshCallback: (suspend () -> Boolean)? = null
    private var refreshJob: Deferred<Boolean>? = null

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
        return refreshInternal()
    }

    /**
     * Forces a token refresh regardless of expiry state (e.g. after a 401).
     * Returns true if refresh was successful, false otherwise.
     */
    suspend fun forceRefreshToken(): Boolean {
        Log.d(TAG, "Forcing token refresh after 401")
        return refreshInternal()
    }

    /**
     * Single-flight token refresh: concurrent callers (e.g. several requests
     * getting a 401 at the same time) all wait on the same in-flight refresh
     * instead of each POSTing to /auth/refresh independently.
     */
    private suspend fun refreshInternal(): Boolean {
        return refreshMutex.withLock {
            val active = refreshJob
            if (active != null) {
                Log.d(TAG, "Token refresh already in progress, awaiting result")
                active.await()
            } else {
                // Run the refresh outside the request's coroutine so a cancelled
                // caller (e.g. a request that timed out) doesn't cancel the shared refresh.
                val job = coroutineScope.async {
                    tokenRefreshCallback?.invoke() ?: false
                }
                refreshJob = job
                job.invokeOnCompletion { refreshJob = null }
                job.await()
            }
        }
    }
}
