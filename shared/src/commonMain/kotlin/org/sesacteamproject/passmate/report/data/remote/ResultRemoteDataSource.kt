package org.sesacteamproject.passmate.report.data.remote

import io.ktor.client.call.body
import io.ktor.client.request.get
import org.sesacteamproject.passmate.core.network.ApiClient
import org.sesacteamproject.passmate.report.data.dto.LearningReportResponse
import org.sesacteamproject.passmate.report.data.dto.SessionResultResponse

// 전송만 담당 — AppResult 변환·매핑은 Repository가 한다 (규칙 §6)
class ResultRemoteDataSource(
    private val apiClient: ApiClient
) {
    suspend fun fetchSessionResult(roomId: Long): SessionResultResponse {
        return apiClient.http.get("${apiClient.baseUrl}/rooms/$roomId/results/me").body()
    }

    suspend fun fetchLearningReport(roomId: Long): LearningReportResponse {
        return apiClient.http.get("${apiClient.baseUrl}/rooms/$roomId/reports/me").body()
    }
}
