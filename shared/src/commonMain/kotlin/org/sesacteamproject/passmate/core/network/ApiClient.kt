package org.sesacteamproject.passmate.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.takeFrom
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.sesacteamproject.passmate.core.storage.TokenStorage

@Serializable
private data class TokenRefreshRequest(val refreshToken: String)

@Serializable
private data class TokenRefreshResponse(val accessToken: String, val refreshToken: String? = null)

class ApiClient(
    private val tokenStorage: TokenStorage,
    val baseUrl: String
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val refreshMutex = Mutex()

    val http: HttpClient = HttpClient {
        expectSuccess = true
        install(ContentNegotiation) {
            json(this@ApiClient.json)
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    private fun bearerToken(): String? {
        return tokenStorage.accessToken() ?: tokenStorage.guestToken
    }

    private fun HttpRequestBuilder.attachAuthorization() {
        val token = bearerToken()

        if (headers[HttpHeaders.Authorization] == null && token != null) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    // 401 → refresh 1회 후 재시도. 백엔드는 토큰 만료를 401로만 응답한다(403은 권한 거부 — 규칙 §8)
    private suspend fun refreshMemberTokens(): Boolean {
        val refreshToken = tokenStorage.refreshToken() ?: return false

        return refreshMutex.withLock {
            if (tokenStorage.refreshToken() != refreshToken) {
                // 다른 요청이 이미 갱신함
                true
            } else {
                try {
                    val response: TokenRefreshResponse = http.post("$baseUrl$REFRESH_PATH") {
                        contentType(ContentType.Application.Json)
                        setBody(TokenRefreshRequest(refreshToken))
                    }.body()

                    tokenStorage.saveMemberTokens(response.accessToken, response.refreshToken ?: refreshToken)
                    true
                } catch (e: ClientRequestException) {
                    tokenStorage.clearMemberTokens()
                    false
                }
            }
        }
    }

    init {
        http.plugin(HttpSend).intercept { request ->
            request.attachAuthorization()

            val canAttemptRefresh = !request.url.buildString().endsWith(REFRESH_PATH) &&
                tokenStorage.accessToken() != null
            var unauthorized: ClientRequestException? = null
            val call = try {
                execute(request)
            } catch (e: ClientRequestException) {
                if (e.response.status == HttpStatusCode.Unauthorized && canAttemptRefresh) {
                    unauthorized = e
                    null
                } else {
                    throw e
                }
            }

            if (call != null && !(call.response.status == HttpStatusCode.Unauthorized && canAttemptRefresh)) {
                call
            } else if (refreshMemberTokens()) {
                val retried = HttpRequestBuilder().takeFrom(request)

                retried.headers.remove(HttpHeaders.Authorization)
                retried.attachAuthorization()
                execute(retried)
            } else {
                call ?: throw unauthorized!!
            }
        }
    }

    companion object {
        // 2026-08-28 백엔드 API 명세서 확정 경로 — 응답의 refreshToken은 미회전 시 생략될 수 있다
        private const val REFRESH_PATH = "/auth/refresh"
    }
}
