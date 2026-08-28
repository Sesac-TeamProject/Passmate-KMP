package org.sesacteamproject.passmate.user.data.remote

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import org.sesacteamproject.passmate.core.network.ApiClient
import org.sesacteamproject.passmate.user.data.dto.MyPageResponse

// 전송만 담당 — AppResult 변환·매핑은 Repository가 한다 (규칙 §6)
class UserRemoteDataSource(
    private val apiClient: ApiClient
) {
    suspend fun fetchMyPage(cursor: String?): MyPageResponse {
        return apiClient.http.get("${apiClient.baseUrl}/users/me/rooms/joined") {
            if (cursor != null) {
                parameter("cursor", cursor)
            }
        }.body()
    }
}
