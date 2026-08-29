package org.sesacteamproject.passmate.auth.data.remote

import io.ktor.client.request.post
import org.sesacteamproject.passmate.core.network.ApiClient

// 전송만 담당 — AppResult 변환은 Repository가 한다 (규칙 §6)
class AuthRemoteDataSource(
    private val apiClient: ApiClient
) {
    // refresh 토큰 무효화 (POST /auth/logout)
    suspend fun logout() {
        apiClient.http.post("${apiClient.baseUrl}/auth/logout")
    }
}
