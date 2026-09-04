package org.sesacteamproject.passmate.auth.data.remote

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.sesacteamproject.passmate.auth.data.dto.DevLoginRequest
import org.sesacteamproject.passmate.auth.data.dto.DevLoginResponse
import org.sesacteamproject.passmate.core.network.ApiClient

// 전송만 담당 — AppResult 변환은 Repository가 한다 (규칙 §6)
class AuthRemoteDataSource(
    private val apiClient: ApiClient
) {
    // refresh 토큰 무효화 (POST /auth/logout)
    suspend fun logout() {
        apiClient.http.post("${apiClient.baseUrl}/auth/logout")
    }

    // 로컬 개발 서버 전용 로그인 (POST /auth/dev-login) — 운영에는 배포되지 않는 API다
    suspend fun devLogin(request: DevLoginRequest): DevLoginResponse {
        return apiClient.http.post("${apiClient.baseUrl}/auth/dev-login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}
