package com.meninocoiso.bscm.data.security

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.util.AttributeKey

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
                val token = plugin.authInterceptor.getAuthTokenBlocking()
                if (token != null) {
                    context.headers.append("Authorization", "Bearer $token")
                }
            }
        }
    }
}
