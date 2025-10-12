package com.meninocoiso.bscm.data.security

import android.net.Uri
import androidx.core.net.toUri
import com.meninocoiso.bscm.data.manager.SecureTokenManager
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class DiscordOAuth @Inject constructor(
    private val tokenManager: SecureTokenManager
) {

    // Generates a random code verifier according to PKCE spec
    @OptIn(ExperimentalEncodingApi::class)
    private fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val code = ByteArray(32)
        secureRandom.nextBytes(code)

        // Base64 URL-safe without padding
        return Base64.UrlSafe.encode(code).trimEnd('=')
    }

    // Creates a code challenge (SHA256 hash of verifier, base64-url-encoded)
    @OptIn(ExperimentalEncodingApi::class)
    private fun generateCodeChallenge(codeVerifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(codeVerifier.toByteArray(Charsets.US_ASCII))

        // Base64 URL-safe without padding
        return Base64.UrlSafe.encode(hash).trimEnd('=')
    }

    // Obtains the OAuth2 flow url with PKCE
    suspend fun getDiscordOAuthUri(): Uri {
        val clientId = "1329849906868912259"
        val redirectUri = "bscm://auth"
        val scope = "identify email"

        // Generate verifier and challenge
        val codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier)

        // Persist code verifier for later use
        println("Generated code verifier: ${codeVerifier.take(10)}...")
        tokenManager.saveCodeVerifier(codeVerifier)

        // Build authorization URL with PKCE parameters
        val authUrl: Uri = "https://discord.com/api/oauth2/authorize".toUri()
            .buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", scope)
            .appendQueryParameter("code_challenge", codeChallenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .build()
        
        return authUrl
    }
}