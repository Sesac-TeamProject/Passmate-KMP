package org.sesacteamproject.passmate.smoke

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import org.sesacteamproject.passmate.core.network.defaultApiBaseUrl

// 스모크 전용 — 세션 진행은 호스트가 REST로 한다(제어는 REST, 전파는 WS).
// 앱은 학생 앱이라 호스트 조작 UseCase가 없으므로 여기서 직접 호출한다.
class HostControl(
    private val hostToken: String
) {
    private val http = HttpClient()

    private suspend fun call(path: String): String {
        val response = http.post("${defaultApiBaseUrl()}$path") {
            header(HttpHeaders.Authorization, "Bearer $hostToken")
        }

        return "${response.status} ${response.bodyAsText().take(200)}"
    }

    suspend fun startSession(roomId: Long) {
        println("[SMOKE][host] start → ${call("/rooms/$roomId/session/start")}")
    }

    suspend fun nextQuestion(roomId: Long) {
        println("[SMOKE][host] next → ${call("/rooms/$roomId/session/next")}")
    }

    suspend fun endSession(roomId: Long) {
        println("[SMOKE][host] end → ${call("/rooms/$roomId/session/end")}")
    }
}
