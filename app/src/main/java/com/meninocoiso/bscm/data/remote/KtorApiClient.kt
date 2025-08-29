package com.meninocoiso.bscm.data.remote

import android.util.Log
import com.meninocoiso.bscm.data.security.AuthInterceptor
import com.meninocoiso.bscm.data.security.AuthPlugin
import com.meninocoiso.bscm.domain.enums.Difficulty
import com.meninocoiso.bscm.domain.enums.Genre
import com.meninocoiso.bscm.domain.enums.OperationType
import com.meninocoiso.bscm.domain.enums.SortOption
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.User
import com.meninocoiso.bscm.domain.model.Version
import com.meninocoiso.bscm.domain.model.auth.AuthRequest
import com.meninocoiso.bscm.domain.model.auth.AuthResponse
import com.meninocoiso.bscm.domain.model.auth.RefreshTokenRequest
import com.meninocoiso.bscm.util.DevelopmentUtils
import com.meninocoiso.bscm.util.KeystoreUtils
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import jakarta.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val TAG = "KtorApiClient"

@Serializable
data class ApiError(val error: String)

class KtorApiClient @Inject constructor(
    private val interceptor: AuthInterceptor
) : ApiClient {
    private val client = HttpClient(Android) {
        /*install(Logging) {
            level = LogLevel.ALL
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("HttpLogging", message)
                }
            }
        }*/
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 10000
            connectTimeoutMillis = 10000
            socketTimeoutMillis = 10000
        }

        // Add authorization header if token is available
        install(AuthPlugin) { 
            authInterceptor = interceptor
        }
        
        defaultRequest {
            // url("https://api-cyb1.onrender.com")
            url {
                protocol = URLProtocol.HTTP
                host = if (DevelopmentUtils.isEmulator()) "10.0.2.2" else "192.168.0.5"
                port = 8080
            }

            val timestamp = System.currentTimeMillis().toString()
            val payload = "$timestamp:"
            val signature = KeystoreUtils.signData(payload)

            headers.append("X-App-Timestamp", timestamp)
            headers.append("X-App-Signature", signature)
            
            contentType(ContentType.Application.Json)
        }
    }

    override suspend fun getUsers(): List<User> {
        return client.get("users").body()
    }

    override suspend fun getUser(id: String): User {
        return client.get("users/$id").body()
    }

    override suspend fun getChart(id: String): Chart {
        return client.get("charts/$id").body()
    }

    override suspend fun getFeedCharts(sortBy: SortOption, limit: Int?, offset: Int): List<Chart> {
        val response = client.get("charts") {
            url {
                parameters.append("sortBy", sortBy.toString())
                limit?.let { parameters.append("limit", it.toString()) }
                parameters.append("offset", offset.toString())
            }
        }

        // Check the response status first
        when (response.status) {
            HttpStatusCode.OK -> {
                return response.body<List<Chart>>()
            }
            HttpStatusCode.TooManyRequests -> {
                val errorResponse = response.body<ApiError>()
                // throw Exception("Rate limited: ${errorResponse.message}")
                throw Exception(errorResponse.error)
            }
            else -> {
                // Handle other error cases
                val errorResponse = try {
                    response.body<ApiError>()
                } catch (e: Exception) {
                    ApiError("Unknown error occurred")
                }
                throw Exception("API Error (${response.status.value}): ${errorResponse.error}")
            }
        }
    }

    override suspend fun getCharts(
        query: String?,
        difficulties: List<Difficulty>?,
        genres: List<Genre>?,
        limit: Int?,
        offset: Int
    ): List<Chart> {
        return client.get("charts"){
            url {
                query?.let { parameters.append("query", it) }
                difficulties?.let { parameters.append("difficulties", it.joinToString(",")) }
                genres?.let { parameters.append("genres", it.joinToString(",")) }
                limit?.let { parameters.append("limit", it.toString()) }
                parameters.append("offset", offset.toString())
            }
        }.body()
    }

    override suspend fun getChartsById(ids: List<String>): List<Chart> {
        return client.get("charts"){
            url {
                parameters.append("ids", ids.joinToString(","))
            }
        }.body()
    }
    
    override suspend fun getSuggestions(query: String, limit: Int?): List<String> {
        return client.get("charts/suggestions"){
            url {
                parameters.append("query", query)
                limit?.let { parameters.append("limit", it.toString()) }
            }
        }.body()
    }

    override suspend fun getLatestVersionsByChartIds(ids: List<String>): List<Version> {
        return client.get("charts/latest-versions"){
            url {
                parameters.append("chartIds", ids.joinToString(","))
            }
        }.body()
    }

    override suspend fun postAnalytics(id: String, operationType: OperationType): Boolean {
        Log.d(TAG, "Posting analytics for chart $id with operation $operationType")
        return client.post("charts/analytics/$id") {
            url {
                parameters.append("type", operationType.toString())
            }
        }.body<Boolean>()
    }

    // Authentication methods
    override suspend fun authenticateWithDiscord(authRequest: AuthRequest): AuthResponse {
        Log.d(TAG, "authenticateWithDiscord: Sending request with code=${authRequest.code.take(10)}..., redirectUri=${authRequest.redirectUri}")

        val response = client.post("auth/discord") {
            setBody(authRequest)
        }

        Log.d(TAG, "authenticateWithDiscord: Response status=${response.status}")

        when (response.status) {
            HttpStatusCode.OK -> {
                Log.d(TAG, "authenticateWithDiscord: Success")
                return response.body<AuthResponse>()
            }
            else -> {
                val errorResponse = try {
                    val error = response.body<ApiError>()
                    Log.e(TAG, "authenticateWithDiscord: Server error response: ${error.error}")
                    error
                } catch (e: Exception) {
                    Log.e(TAG, "authenticateWithDiscord: Failed to parse error response", e)
                    ApiError("Authentication failed - unable to parse server response")
                }
                throw Exception("Auth Error (${response.status.value}): ${errorResponse.error}")
            }
        }
    }

    override suspend fun refreshToken(refreshRequest: RefreshTokenRequest): AuthResponse {
        val response = client.post("auth/refresh") {
            setBody(refreshRequest)
        }

        when (response.status) {
            HttpStatusCode.OK -> {
                return response.body<AuthResponse>()
            }
            else -> {
                val errorResponse = try {
                    response.body<ApiError>()
                } catch (e: Exception) {
                    ApiError("Token refresh failed")
                }
                throw Exception("Refresh Error (${response.status.value}): ${errorResponse.error}")
            }
        }
    }

    override suspend fun getCurrentUser(): User {
        val response = client.get("auth/me")

        when (response.status) {
            HttpStatusCode.OK -> {
                return response.body<User>()
            }
            else -> {
                val errorResponse = try {
                    response.body<ApiError>()
                } catch (e: Exception) {
                    ApiError("Failed to get user info")
                }
                throw Exception("User Error (${response.status.value}): ${errorResponse.error}")
            }
        }
    }
}