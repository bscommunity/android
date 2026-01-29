package com.meninocoiso.bscm.data.security

import android.content.Context
import com.meninocoiso.bscm.util.SecurityUtils
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.util.AttributeKey
import kotlinx.coroutines.runBlocking

class AuthPlugin private constructor(
    private val authInterceptor: AuthInterceptor,
    private val context: Context
) {

    class Config {
        var authInterceptor: AuthInterceptor? = null
        var context: Context? = null
    }

    companion object : HttpClientPlugin<Config, AuthPlugin> {
        override val key: AttributeKey<AuthPlugin> = AttributeKey("AuthPlugin")

        override fun prepare(block: Config.() -> Unit): AuthPlugin {
            val config = Config().apply(block)
            return AuthPlugin(
                authInterceptor = config.authInterceptor
                    ?: throw IllegalArgumentException("AuthInterceptor must be provided"),
                context = config.context
                    ?: throw IllegalArgumentException("Context must be provided")
            )
        }

        override fun install(plugin: AuthPlugin, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.State) {
                // Get app signature
                val appSignature = SecurityUtils.getAppSignature(plugin.context)

                // Create payload with timestamp and signature
                val timestamp = System.currentTimeMillis().toString()
                val payload = "$timestamp:$appSignature"

                // Sign the payload
                val hmacSignature = SecurityUtils.signData(plugin.context, payload)

                // Add security headers
                context.headers.append("X-App-Signature", appSignature)
                context.headers.append("X-Timestamp", timestamp)
                context.headers.append("X-HMAC", hmacSignature)

                // Add JWT token if available
                val token = runBlocking { plugin.authInterceptor.getAuthToken() }
                if (token != null) {
                    context.headers.append("Authorization", "Bearer $token")
                }

                proceed()
            }
        }
    }
}