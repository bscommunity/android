package com.meninocoiso.bscm.data.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_tokens")

@Singleton
class SecureTokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
    private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
    private val CODE_VERIFIER_KEY = stringPreferencesKey("code_verifier")

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = accessToken
            preferences[REFRESH_TOKEN_KEY] = refreshToken
        }
    }

    suspend fun getAccessToken(): String? {
        return context.dataStore.data
            .map { preferences -> preferences[ACCESS_TOKEN_KEY] }
            .first()
    }

    suspend fun getRefreshToken(): String? {
        return context.dataStore.data
            .map { preferences -> preferences[REFRESH_TOKEN_KEY] }
            .first()
    }

    suspend fun saveCodeVerifier(codeVerifier: String) {
        context.dataStore.edit { preferences ->
            preferences[CODE_VERIFIER_KEY] = codeVerifier
        }
    }

    suspend fun getCodeVerifier(): String? {
        return context.dataStore.data
            .map { preferences -> preferences[CODE_VERIFIER_KEY] }
            .first()
    }

    suspend fun clearCodeVerifier() {
        context.dataStore.edit { preferences ->
            preferences.remove(CODE_VERIFIER_KEY)
        }
    }

    suspend fun clearTokens() {
        context.dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)
            preferences.remove(REFRESH_TOKEN_KEY)
            preferences.remove(CODE_VERIFIER_KEY)
        }
    }

    suspend fun isLoggedIn(): Boolean {
        return getAccessToken() != null
    }

    // --- Reactive flows ----------------------------------------------------
    // Fluxo contínuo do access token (null quando não logado)
    fun accessTokenFlow(): Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN_KEY] }
    // Fluxo booleano de estado de login
    fun isLoggedInFlow(): Flow<Boolean> = accessTokenFlow().map { it != null }
}
