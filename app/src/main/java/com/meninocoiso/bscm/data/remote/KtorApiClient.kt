package com.meninocoiso.bscm.data.remote

import android.content.Context
import android.util.Log
import com.meninocoiso.bscm.data.security.AuthInterceptor
import com.meninocoiso.bscm.data.security.AuthPlugin
import com.meninocoiso.bscm.data.remote.dto.activity.ActivityEntry
import com.meninocoiso.bscm.data.remote.dto.collection.CreateCollectionItemRequest
import com.meninocoiso.bscm.data.remote.dto.collection.CreateCollectionRequest
import com.meninocoiso.bscm.data.remote.dto.collection.UpdateCollectionRequest
import com.meninocoiso.bscm.data.remote.dto.user.UserProfileResponse
import com.meninocoiso.bscm.domain.enums.Difficulty
import com.meninocoiso.bscm.domain.enums.Genre
import com.meninocoiso.bscm.domain.enums.OperationOption
import com.meninocoiso.bscm.domain.enums.SortOption
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Collection
import com.meninocoiso.bscm.domain.model.User
import com.meninocoiso.bscm.domain.model.Version
import com.meninocoiso.bscm.domain.model.auth.AuthRequest
import com.meninocoiso.bscm.domain.model.auth.AuthResponse
import com.meninocoiso.bscm.domain.model.auth.RefreshTokenRequest
import com.meninocoiso.bscm.domain.model.internal.ContributionCategory
import com.meninocoiso.bscm.util.DevelopmentUtils
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.delete
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import jakarta.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.ktor.http.ContentType as KtorContentType

private const val TAG = "KtorApiClient"

@Serializable
data class ApiError(val error: String)

class KtorApiClient @Inject constructor(
    private val context: Context,
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
            context = this@KtorApiClient.context
        }
        
        defaultRequest {
            // url("https://api-cyb1.onrender.com")
            url {
                protocol = URLProtocol.HTTP
                host = if (DevelopmentUtils.isEmulator()) "10.0.2.2" else "192.168.0.4"
                port = 8080
            }
            contentType(KtorContentType.Application.Json)
        }
    }

    override suspend fun getChart(id: String): Chart {
        return client.get("charts/$id").body()
    }

    override suspend fun getCharts(
        query: String?,
        sortBy: SortOption?,
        difficulties: List<Difficulty>?,
        genres: List<Genre>?,
        limit: Int?,
        offset: Int
    ): List<Chart> {
        val response = client.get("charts"){
            url {
                query?.let { parameters.append("query", it) }
                sortBy?.let { parameters.append("sortBy", it.toString()) }
                difficulties?.let { parameters.append("difficulties", it.joinToString(",")) }
                genres?.let { parameters.append("genres", it.joinToString(",")) }
                limit?.let { parameters.append("limit", it.toString()) }
                parameters.append("offset", offset.toString())
            }
        }

        Log.d(TAG, "getCharts: Response status=${response.status}, response: ${response.body<String>()}")

        // Check the response status first
        when (response.status) {
            HttpStatusCode.OK -> {
                val body = response.body<Pair<List<Chart>, Int?>>()
                return body.first
            }
            HttpStatusCode.RequestTimeout -> {
                Log.e(TAG, "Request timed out")
                throw Exception("Request timed out. Please try again later.")
            }
            HttpStatusCode.TooManyRequests -> {
                val errorResponse = response.body<ApiError>()
                Log.e(TAG, "Rate limit exceeded: ${errorResponse.error}")
                throw Exception(errorResponse.error)
            }
            else -> {
                // Handle other error cases
                val errorResponse = try {
                    Log.e(TAG, "Error response body: ${response.body<String>()}")
                    response.body<ApiError>()
                } catch (e: Exception) {
                    ApiError("Unknown error occurred")
                }
                throw Exception("API Error (${response.status.value}): ${errorResponse.error}")
            }
        }
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

    override suspend fun postAnalytics(id: String, operationOption: OperationOption): Boolean {
        Log.d(TAG, "Posting analytics for chart $id with operation $operationOption")
        return client.post("charts/analytics/$id") {
            url {
                parameters.append("type", operationOption.toString())
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
        Log.d(TAG, "authenticateWithDiscord: Response =${response.body<String>()}")

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

    override suspend fun getUsers(search: String?): List<User> {
        return client.get("users") {
            url {
                search?.let { parameters.append("search", it) }
            }
        }.body()
    }

    override suspend fun getUser(id: String): User {
        return client.get("users/$id").body()
    }

    override suspend fun getUserProfile(id: String): UserProfileResponse {
        return client.get("users/$id").body()
    }

    override suspend fun getUserProfileByUsername(username: String): UserProfileResponse {
        return client.get("users/username/$username").body()
    }

    override suspend fun getUserActivity(id: String, limit: Int?, offset: Int?): List<ActivityEntry> {
        return client.get("users/$id/activity") {
            url {
                limit?.let { parameters.append("limit", it.toString()) }
                offset?.let { parameters.append("offset", it.toString()) }
            }
        }.body()
    }

    override suspend fun getUserCharts(id: String, limit: Int?, offset: Int?): List<Chart> {
        return client.get("users/$id/charts") {
            url {
                limit?.let { parameters.append("limit", it.toString()) }
                offset?.let { parameters.append("offset", it.toString()) }
            }
        }.body()
    }

    override suspend fun followUser(id: String): Boolean {
        val response = client.post("users/$id/follow")
        return response.status == HttpStatusCode.OK
    }

    override suspend fun unfollowUser(id: String): Boolean {
        val response = client.delete("users/$id/follow")
        return response.status == HttpStatusCode.OK
    }

    override suspend fun getMyProfile(): UserProfileResponse {
        return client.get("me/profile").body()
    }

    override suspend fun getMyActivity(limit: Int?, offset: Int?): List<ActivityEntry> {
        return client.get("me/activity") {
            url {
                limit?.let { parameters.append("limit", it.toString()) }
                offset?.let { parameters.append("offset", it.toString()) }
            }
        }.body()
    }

    override suspend fun getMyLikes(limit: Int?, offset: Int?): List<Chart> {
        return client.get("me/likes") {
            url {
                limit?.let { parameters.append("limit", it.toString()) }
                offset?.let { parameters.append("offset", it.toString()) }
            }
        }.body()
    }

    override suspend fun getMyBookmarks(limit: Int?, offset: Int?): List<Chart> {
        return client.get("me/bookmarks") {
            url {
                limit?.let { parameters.append("limit", it.toString()) }
                offset?.let { parameters.append("offset", it.toString()) }
            }
        }.body()
    }

    override suspend fun getUserCollections(limit: Int?, offset: Int?): List<Collection> {
        return client.get("collections") {
            url {
                limit?.let { parameters.append("limit", it.toString()) }
                offset?.let { parameters.append("offset", it.toString()) }
            }
        }.body()
    }

    override suspend fun createCollection(name: String, isPublic: Boolean): Collection {
        return client.post("collections") {
            setBody(CreateCollectionRequest(name = name, isPublic = isPublic))
        }.body()
    }

    override suspend fun updateCollection(collectionId: String, name: String?, isPublic: Boolean?): Boolean {
        val response = client.put("collections/$collectionId") {
            setBody(UpdateCollectionRequest(name = name, isPublic = isPublic))
        }
        return response.status == HttpStatusCode.OK
    }

    override suspend fun deleteCollection(collectionId: String): Boolean {
        val response = client.delete("collections/$collectionId")
        return response.status == HttpStatusCode.OK
    }

    override suspend fun getCollectionItems(
        collectionId: String,
        contentType: String?,
        limit: Int?,
        offset: Int?
    ): List<Chart> {
        return client.get("collections/$collectionId/items") {
            url {
                contentType?.let { parameters.append("contentType", it) }
                limit?.let { parameters.append("limit", it.toString()) }
                offset?.let { parameters.append("offset", it.toString()) }
            }
        }.body()
    }

    override suspend fun addItemToCollection(collectionId: String, contentId: String): Boolean {
        val response = client.post("collections/$collectionId/items") {
            setBody(mapOf("contentId" to contentId))
        }
        return response.status == HttpStatusCode.OK
    }

    override suspend fun removeItemFromCollection(collectionId: String, contentId: String): Boolean {
        val response = client.delete("collections/$collectionId/items/$contentId")
        return response.status == HttpStatusCode.OK
    }

    override suspend fun batchProcessInteractions(interactions: List<CreateCollectionItemRequest>): Boolean {
        val response = client.post("collections/batch") {
            setBody(interactions)
        }
        return response.status == HttpStatusCode.OK
    }

    /**
     * Fetches the list of contributors from the remote server.
     * @return A list of ContributionCategory objects.
     */
    override suspend fun getContributors(): List<ContributionCategory> {
        return try {
            client.get("https://bscm.netlify.app/contributors.json").body()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch contributors", e)
            emptyList()
        }
    }
}