package com.meninocoiso.beatstarcommunity.data.remote

import com.meninocoiso.beatstarcommunity.domain.enums.Difficulty
import com.meninocoiso.beatstarcommunity.domain.enums.Genre
import com.meninocoiso.beatstarcommunity.domain.enums.SortOption
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import com.meninocoiso.beatstarcommunity.domain.model.User
import com.meninocoiso.beatstarcommunity.domain.model.Version
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import jakarta.inject.Inject
import kotlinx.serialization.json.Json

class KtorApiClient @Inject constructor() : ApiClient {
    private val client = HttpClient(Android) {
        /*install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("HttpLogging:", message)
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
        }
        defaultRequest {
            url("https://api-cyb1.onrender.com")
            /*url {
                protocol = URLProtocol.HTTP
                host = if (DevelopmentUtils.isEmulator()) "10.0.2.2" else "192.168.0.11"
                port = 8080
            }*/
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
        return client.get("charts"){
            url {
                parameters.append("fetchContributors", "true")
                parameters.append("sortBy", sortBy.toString())
                limit?.let { parameters.append("limit", it.toString()) }
                parameters.append("offset", offset.toString())
            }
        }.body()
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
                parameters.append("fetchContributors", "true")
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
}