package com.meninocoiso.bscm.data.security

import android.util.Log
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.plugin
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

private const val TAG = "TokenRefreshPlugin"

/**
 * Ktor plugin to handle 401 Unauthorized responses by refreshing the token
 * and retrying the request once.
 */
val TokenRefreshPlugin = createClientPlugin("TokenRefreshPlugin", ::TokenRefreshConfig) {
    val config = pluginConfig

    client.plugin(HttpSend).intercept { request ->
        val originalCall = execute(request)

        // Check if we got a 401 Unauthorized response
        if (originalCall.response.status == HttpStatusCode.Unauthorized) {
            Log.d(TAG, "Received 401 Unauthorized, attempting token refresh")

            // Try to refresh the token (single-flight handled by AuthInterceptor)
            val refreshed = try {
                config.authInterceptor?.forceRefreshToken() ?: false
            } catch (e: Exception) {
                Log.e(TAG, "Exception during token refresh: ${e.message}", e)
                false
            }

            if (refreshed) {
                Log.d(TAG, "Token refreshed successfully, retrying request")
                // The request pipeline (AuthPlugin) only runs its State phase once,
                // so the retried builder still carries the stale Authorization header.
                // Replace it with the freshly issued token before re-sending.
                val newToken = config.authInterceptor?.getAuthToken()
                if (newToken != null) {
                    request.headers.remove(HttpHeaders.Authorization)
                    request.headers.append(HttpHeaders.Authorization, "Bearer $newToken")
                }
                return@intercept execute(request)
            } else {
                Log.e(TAG, "Token refresh failed, returning original 401 response")
            }
        }

        originalCall
    }
}

class TokenRefreshConfig {
    var authInterceptor: AuthInterceptor? = null
}



