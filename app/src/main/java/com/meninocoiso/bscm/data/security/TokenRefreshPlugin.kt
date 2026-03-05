package com.meninocoiso.bscm.data.security

import android.util.Log
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.plugin
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
                // Retry the request with the new token
                // The AuthPlugin will automatically add the new token to the headers
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



