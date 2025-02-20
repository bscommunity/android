package com.meninocoiso.beatstarcommunity.data.remote

import com.meninocoiso.beatstarcommunity.data.remote.dto.ContributorUserDto
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import com.meninocoiso.beatstarcommunity.domain.model.User
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import jakarta.inject.Inject
import kotlinx.serialization.json.Json

class KtorApiClient @Inject constructor() : ApiClient {
    private val client = HttpClient(Android) {
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
            url {
                protocol = URLProtocol.HTTP
                host = "10.0.2.2"
                port = 8080
            }
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

    override suspend fun getCharts(): List<Chart> {
        return client.get("charts").body()
    }
}