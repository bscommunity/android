package com.meninocoiso.bscm.data.security

import com.meninocoiso.bscm.util.KeystoreUtils
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.util.AttributeKey
import kotlinx.coroutines.runBlocking

class AuthPlugin private constructor(
    private val authInterceptor: AuthInterceptor
) {
    
    class Config {
        var authInterceptor: AuthInterceptor? = null
    }

    companion object : HttpClientPlugin<Config, AuthPlugin> {
        override val key: AttributeKey<AuthPlugin> = AttributeKey("AuthPlugin")

        override fun prepare(block: Config.() -> Unit): AuthPlugin {
            val config = Config().apply(block)
            return AuthPlugin(
                authInterceptor = config.authInterceptor 
                    ?: throw IllegalArgumentException("AuthInterceptor must be provided")
            )
        }

        override fun install(plugin: AuthPlugin, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.State) {
                // --- HMAC Signature ---
                val timestamp = System.currentTimeMillis().toString()
                val payload = "$timestamp:"
                val signature = KeystoreUtils.signData(payload)
                context.headers.append("X-App-Timestamp", timestamp)
                context.headers.append("X-App-Signature", signature)

                // --- JWT Token ---
                val token = runBlocking { plugin.authInterceptor.getAuthToken() }
                if (token != null) {
                    context.headers.append("Authorization", "Bearer $token")
                }

                // --- DEBUG PRINT HEADERS ---
                /*val headersString = context.headers.entries().joinToString("\n") { (key, values) ->
                    "$key: ${values.joinToString(", ")}"
                }
                Log.d("AuthPlugin", "=== Request Headers ===\n$headersString\n========================")*/

                proceed()
            }
        }
    }
}
