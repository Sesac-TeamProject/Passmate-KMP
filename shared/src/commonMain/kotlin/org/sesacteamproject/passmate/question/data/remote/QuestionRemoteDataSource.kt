package org.sesacteamproject.passmate.question.data.remote

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import org.sesacteamproject.passmate.core.network.ApiClient
import org.sesacteamproject.passmate.question.data.dto.QuestionSetsResponse

// 전송만 담당 — AppResult 변환·매핑은 Repository가 한다 (규칙 §6)
class QuestionRemoteDataSource(
    private val apiClient: ApiClient
) {
    suspend fun fetchMySets(status: String?, cursor: String?): QuestionSetsResponse {
        return apiClient.http.get("${apiClient.baseUrl}/question-sets") {
            if (status != null) {
                parameter("status", status)
            }
            if (cursor != null) {
                // 서버는 page/size 기반이다 — 매퍼가 다음 페이지 번호를 커서 자리에 싣는다
                parameter("page", cursor)
            }
        }.body()
    }
}
