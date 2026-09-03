package org.sesacteamproject.passmate.session.data.remote

import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import org.sesacteamproject.passmate.core.network.ApiClient
import org.sesacteamproject.passmate.session.data.dto.ScreenLockRequest
import org.sesacteamproject.passmate.core.model.ServerClock
import org.sesacteamproject.passmate.session.data.dto.SessionSnapshotResponse
import org.sesacteamproject.passmate.session.data.dto.StartSessionResponse
import org.sesacteamproject.passmate.session.data.dto.SubmissionsResponse
import org.sesacteamproject.passmate.session.data.dto.SubmitAnswerRequest
import org.sesacteamproject.passmate.session.data.dto.SubmitAnswerResponse
import org.sesacteamproject.passmate.session.data.dto.VoiceHintsResponse

// 전송만 담당 — AppResult 변환·매핑은 Repository가 한다 (규칙 §6)
class SessionRemoteDataSource(
    private val apiClient: ApiClient
) {
    // 스냅샷 본문에 서버 시각이 없어 응답 Date 헤더를 함께 돌려준다 (§2-1-2·§5)
    suspend fun fetchSnapshot(roomId: Long): SnapshotWithServerTime {
        val response = apiClient.http.get("${apiClient.baseUrl}/rooms/$roomId/session")

        return SnapshotWithServerTime(
            snapshot = response.body(),
            serverTime = ServerClock.toServerLocalIso(response.headers[HttpHeaders.Date])
        )
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

    // PTT 클립 업로드 (M-T2) — multipart: audio(클립)+durationMs. 앱 포맷=audio/mp4(m4a/AAC)
    suspend fun publishHint(
        roomId: Long,
        audioBytes: ByteArray,
        mimeType: String,
        fileName: String,
        durationMs: Long
    ): VoiceHintsResponse.Entry {
        return apiClient.http.submitFormWithBinaryData(
            url = "${apiClient.baseUrl}/rooms/$roomId/session/hints",
            formData = formData {
                append(
                    "audio",
                    audioBytes,
                    Headers.build {
                        append(HttpHeaders.ContentType, mimeType)
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    }
                )
                append("durationMs", durationMs.toString())
            }
        ).body()
    }

    // ── 호스트 세션 제어 (M-T2 리모컨) — contracts §Session ──

    suspend fun startSession(roomId: Long): StartSessionResponse {
        return apiClient.http.post("${apiClient.baseUrl}/rooms/$roomId/session/start").body()
    }

    suspend fun nextQuestion(roomId: Long) {
        apiClient.http.post("${apiClient.baseUrl}/rooms/$roomId/session/next")
    }

    suspend fun endCurrentQuestion(roomId: Long) {
        apiClient.http.post("${apiClient.baseUrl}/rooms/$roomId/session/current/end")
    }

    suspend fun endSession(roomId: Long) {
        apiClient.http.post("${apiClient.baseUrl}/rooms/$roomId/session/end")
    }

    suspend fun setScreenLock(roomId: Long, request: ScreenLockRequest) {
        apiClient.http.put("${apiClient.baseUrl}/rooms/$roomId/session/lock") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun fetchSubmissions(roomId: Long): SubmissionsResponse {
        return apiClient.http.get("${apiClient.baseUrl}/rooms/$roomId/session/current/submissions").body()
    }

    // 전송 계층이 본문과 응답 헤더를 함께 넘기기 위한 묶음 (규칙 §6 — 매핑은 Repository가 한다)
    data class SnapshotWithServerTime(
        val snapshot: SessionSnapshotResponse,
        val serverTime: String?
    )
}
