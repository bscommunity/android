package com.meninocoiso.bscm.data.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_tokens")

@Singleton
class SecureTokenManager @Inject constructor(
    private val context: Context,
    private val cryptoManager: CryptoManager
) {
    private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
    private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
    private val CODE_VERIFIER_KEY = stringPreferencesKey("code_verifier")
    private val TOKEN_EXPIRY_KEY = stringPreferencesKey("token_expiry")

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        val encryptedAccessToken = cryptoManager.encrypt(accessToken)
        val encryptedRefreshToken = cryptoManager.encrypt(refreshToken)
        // Set expiry to 50 minutes from now (tokens typically expire in 1 hour)
        val expiryTime = System.currentTimeMillis() + (50 * 60 * 1000)
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = encryptedAccessToken
            preferences[REFRESH_TOKEN_KEY] = encryptedRefreshToken
            preferences[TOKEN_EXPIRY_KEY] = expiryTime.toString()
        }
    }

    suspend fun getAccessToken(): String? {
        val encrypted = context.dataStore.data
            .map { preferences -> preferences[ACCESS_TOKEN_KEY] }
            .first()
        return encrypted?.let {
            try { cryptoManager.decrypt(it) } catch (_: Exception) { null }
        }
    }

    suspend fun getRefreshToken(): String? {
        val encrypted = context.dataStore.data
            .map { preferences -> preferences[REFRESH_TOKEN_KEY] }
            .first()
        return encrypted?.let {
            try { cryptoManager.decrypt(it) } catch (_: Exception) { null }
        }
    }

    suspend fun saveCodeVerifier(codeVerifier: String) {
        println("Saving code verifier: [0m${codeVerifier.take(10)}...")
        val encryptedCodeVerifier = cryptoManager.encrypt(codeVerifier)
        context.dataStore.edit { preferences ->
            preferences[CODE_VERIFIER_KEY] = encryptedCodeVerifier
        }
    }

    suspend fun getCodeVerifier(): String? {
        println("Retrieving code verifier...")
        val encrypted = context.dataStore.data
            .map { preferences -> preferences[CODE_VERIFIER_KEY] }
            .first()
        return encrypted?.let {
            try { cryptoManager.decrypt(it) } catch (_: Exception) { null }
        }
    }

    suspend fun clearCodeVerifier() {
        println("Clearing code verifier...")
        context.dataStore.edit { preferences ->
            preferences.remove(CODE_VERIFIER_KEY)
        }
    }

    suspend fun clearTokens() {
        context.dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)
            preferences.remove(REFRESH_TOKEN_KEY)
            preferences.remove(CODE_VERIFIER_KEY)
            preferences.remove(TOKEN_EXPIRY_KEY)
        }
    }

    suspend fun isTokenExpired(): Boolean {
        val expiryTimeStr = context.dataStore.data
            .map { preferences -> preferences[TOKEN_EXPIRY_KEY] }
            .first()

        if (expiryTimeStr == null) return true

        val expiryTime = expiryTimeStr.toLongOrNull() ?: return true
        return System.currentTimeMillis() >= expiryTime
    }

    suspend fun getTokenExpiryTime(): Long? {
        val expiryTimeStr = context.dataStore.data
            .map { preferences -> preferences[TOKEN_EXPIRY_KEY] }
            .first()
        return expiryTimeStr?.toLongOrNull()
    }

    suspend fun isLoggedIn(): Boolean {
        return getAccessToken() != null
    }

    // --- Reactive flows ----------------------------------------------------
    fun accessTokenFlow(): Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN_KEY]?.let { enc -> try { cryptoManager.decrypt(enc) } catch (_: Exception) { null } } }
    fun isLoggedInFlow(): Flow<Boolean> = accessTokenFlow().map { it != null }
}
