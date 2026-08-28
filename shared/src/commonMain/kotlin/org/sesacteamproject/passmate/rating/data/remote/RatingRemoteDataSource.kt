package org.sesacteamproject.passmate.rating.data.remote

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.sesacteamproject.passmate.core.network.ApiClient
import org.sesacteamproject.passmate.rating.data.dto.SubmitRatingRequest

// 전송만 담당 — AppResult 변환·매핑은 Repository가 한다 (규칙 §6)
class RatingRemoteDataSource(
    private val apiClient: ApiClient
) {
    suspend fun submitRating(roomId: Long, request: SubmitRatingRequest) {
        apiClient.http.post("${apiClient.baseUrl}/rooms/$roomId/ratings") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }
}
