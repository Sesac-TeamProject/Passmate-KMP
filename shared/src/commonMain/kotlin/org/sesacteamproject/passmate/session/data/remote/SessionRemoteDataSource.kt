package org.sesacteamproject.passmate.session.data.remote

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.sesacteamproject.passmate.core.network.ApiClient
import org.sesacteamproject.passmate.session.data.dto.SessionSnapshotResponse
import org.sesacteamproject.passmate.session.data.dto.SubmitAnswerRequest
import org.sesacteamproject.passmate.session.data.dto.SubmitAnswerResponse
import org.sesacteamproject.passmate.session.data.dto.VoiceHintsResponse

// 전송만 담당 — AppResult 변환·매핑은 Repository가 한다 (규칙 §6)
class SessionRemoteDataSource(
    private val apiClient: ApiClient
) {
    suspend fun fetchSnapshot(roomId: Long): SessionSnapshotResponse {
        return apiClient.http.get("${apiClient.baseUrl}/rooms/$roomId/session").body()
    }

    suspend fun submitAnswer(roomId: Long, questionId: Long, request: SubmitAnswerRequest): SubmitAnswerResponse {
        return apiClient.http.post(
            "${apiClient.baseUrl}/rooms/$roomId/session/questions/$questionId/answers"
        ) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun fetchVoiceHints(roomId: Long): VoiceHintsResponse {
        return apiClient.http.get("${apiClient.baseUrl}/rooms/$roomId/session/hints").body()
    }
}
