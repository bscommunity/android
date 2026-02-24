package com.meninocoiso.bscm.data.remote

import android.content.Context
import android.util.Log
import com.meninocoiso.bscm.data.manager.SecureTokenManager
import com.meninocoiso.bscm.data.remote.dto.activity.ActivityItemResponse
import com.meninocoiso.bscm.data.remote.dto.collection.CreateCollectionItemRequest
import com.meninocoiso.bscm.data.remote.dto.collection.CreateCollectionRequest
import com.meninocoiso.bscm.data.remote.dto.collection.UpdateCollectionRequest
import com.meninocoiso.bscm.data.remote.dto.user.UserProfileResponse
import com.meninocoiso.bscm.data.security.AuthInterceptor
import com.meninocoiso.bscm.data.security.AuthPlugin
import com.meninocoiso.bscm.data.security.TokenRefreshPlugin
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
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import jakarta.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.ktor.http.ContentType as KtorContentType

private const val TAG = "KtorApiClient"

@Serializable
data class ApiError(val error: String)

class ApiException(val status: HttpStatusCode, override val message: String) : Exception(message)

class KtorApiClient @Inject constructor(
    private val context: Context,
    private val interceptor: AuthInterceptor,
    private val tokenManager: SecureTokenManager
) : ApiClient {

    private val errorJson = Json {
        ignoreUnknownKeys = true
    }

    init {
        interceptor.setTokenRefreshCallback {
            refreshTokens()
        }
    }

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

        HttpResponseValidator {
            validateResponse { response ->
                if (response.status.value >= 400) {
                    val message = parseErrorMessage(response)
                    throw ApiException(response.status, message)
                }
            }
        }

        // Install token refresh plugin to handle 401 errors
        install(TokenRefreshPlugin) {
            authInterceptor = interceptor
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

    override suspend fun getChartByContentId(contentId: String): Chart {
        return client.get("charts/content/$contentId").body()
    }

    override suspend fun getCharts(
        query: String?,
        sortBy: SortOption?,
        difficulties: List<Difficulty>?,
        genres: List<Genre>?,
        limit: Int?,
        offset: Int
    ): List<Chart> {
        val body = client.get("charts"){
            url {
                query?.let { parameters.append("query", it) }
                sortBy?.let { parameters.append("sortBy", it.toString()) }
                difficulties?.let { parameters.append("difficulties", it.joinToString(",")) }
                genres?.let { parameters.append("genres", it.joinToString(",")) }
                limit?.let { parameters.append("limit", it.toString()) }
                parameters.append("offset", offset.toString())
            }
        }.body<Pair<List<Chart>, Int?>>()

        return body.first
    }

    override suspend fun getChartsByIds(ids: List<String>): List<Chart> {
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
        }.body<AuthResponse>()

        Log.d(TAG, "authenticateWithDiscord: Success")
        return response
    }

    override suspend fun refreshToken(refreshRequest: RefreshTokenRequest): AuthResponse {
        return client.post("auth/refresh") {
            setBody(refreshRequest)
        }.body()
    }

    override suspend fun getCurrentUser(): User {
        return client.get("auth/me").body()
    }

    /**
     * Refresh the access token (single-flight handled by AuthInterceptor).
     * Returns true if refresh was successful, false otherwise.
     */
    private suspend fun refreshTokens(): Boolean {
        return try {
            Log.d(TAG, "Attempting to refresh token")
            val refreshToken = tokenManager.getRefreshToken()

            if (refreshToken.isNullOrEmpty()) {
                Log.e(TAG, "No refresh token available")
                return false
            }

            val request = RefreshTokenRequest(refreshToken)
            val response = refreshToken(request)

            // Save the new tokens
            tokenManager.saveTokens(response.accessToken, response.refreshToken)
            Log.d(TAG, "Token refreshed successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Token refresh failed: ${e.message}", e)
            // If refresh fails, clear tokens to force re-authentication
            tokenManager.clearTokens()
            false
        }
    }

    private suspend fun parseErrorMessage(response: HttpResponse): String {
        val bodyText = try {
            response.bodyAsText()
        } catch (_: Exception) {
            null
        }

        if (bodyText.isNullOrBlank()) {
            return when (response.status) {
                HttpStatusCode.RequestTimeout -> "Request timed out. Please try again later."
                HttpStatusCode.TooManyRequests -> "Rate limit exceeded. Please try again later."
                HttpStatusCode.Unauthorized -> "Token is not valid or has expired"
                else -> "HTTP ${response.status.value}"
            }
        }

        return try {
            errorJson.decodeFromString<ApiError>(bodyText).error
        } catch (_: Exception) {
            bodyText
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

    override suspend fun getUserActivity(id: String, limit: Int?, offset: Int?): List<ActivityItemResponse> {
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
        return response.status.isSuccess()
    }

    override suspend fun unfollowUser(id: String): Boolean {
        val response = client.delete("users/$id/follow")
        return response.status.isSuccess()
    }

    override suspend fun getMyProfile(): UserProfileResponse {
        return client.get("me/profile").body()
    }

    override suspend fun getMyCollections(limit: Int?, offset: Int?): List<Collection> {
        return client.get("me/collections") {
            url {
                limit?.let { parameters.append("limit", it.toString()) }
                offset?.let { parameters.append("offset", it.toString()) }
            }
        }.body()
    }

    override suspend fun getMyActivity(limit: Int?, offset: Int?): List<ActivityItemResponse> {
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

    override suspend fun addLike(contentId: String): Boolean {
        val response = client.post("me/likes/$contentId")
        return response.status.isSuccess()
    }

    override suspend fun removeLike(contentId: String): Boolean {
        val response = client.delete("me/likes/$contentId")
        return response.status.isSuccess()
    }

    override suspend fun addBookmark(contentId: String): Boolean {
        val response = client.post("me/bookmarks/$contentId")
        return response.status.isSuccess()
    }

    override suspend fun removeBookmark(contentId: String): Boolean {
        val response = client.delete("me/bookmarks/$contentId")
        return response.status.isSuccess()
    }

    override suspend fun getUserCollections(userId: String, limit: Int?, offset: Int?): List<Collection> {
        return client.get("collections/$userId") {
            url {
                limit?.let { parameters.append("limit", it.toString()) }
                offset?.let { parameters.append("offset", it.toString()) }
            }
        }.body()
    }

    override suspend fun getCollection(collectionId: String): Collection {
        return client.get("collections/$collectionId").body()
    }

    override suspend fun getCollectionBySlug(username: String, slug: String): Collection {
        return client.get("collections/slug/$username/$slug").body()
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
        return response.status.isSuccess()
    }

    override suspend fun deleteCollection(collectionId: String): Boolean {
        val response = client.delete("collections/$collectionId")
        return response.status.isSuccess()
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
        Log.d(TAG, "Add item to collection response: ${response.status}, body: ${response.bodyAsText()}")
        return response.status.isSuccess()
    }

    override suspend fun removeItemFromCollection(collectionId: String, contentId: String): Boolean {
        val response = client.delete("collections/$collectionId/items/$contentId")
        return response.status.isSuccess()
    }

    override suspend fun batchProcessInteractions(interactions: List<CreateCollectionItemRequest>): Boolean {
        val response = client.post("collections/batch") {
            setBody(interactions)
        }
        println("Batch process response: ${response.status}, body: ${response.bodyAsText()}")
        return response.status.isSuccess()
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