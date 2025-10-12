package com.meninocoiso.bscm.data.security

import com.meninocoiso.bscm.data.manager.SecureTokenManager
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: SecureTokenManager
) {
    
    suspend fun getAuthToken(): String? {
        return tokenManager.getAccessToken()
    }
    
    fun getAuthTokenBlocking(): String? {
        return runBlocking {
            getAuthToken()
        }
    }
}
