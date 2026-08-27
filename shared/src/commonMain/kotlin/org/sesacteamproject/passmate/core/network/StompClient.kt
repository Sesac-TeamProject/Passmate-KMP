package org.sesacteamproject.passmate.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.StompClient as KrossbowStompClient
import org.hildan.krossbow.websocket.ktor.KtorWebSocketClient
import org.sesacteamproject.passmate.core.storage.TokenStorage

// contracts/websocket-events.md — CONNECT 헤더에 Authorization: Bearer <JWT|게스트 토큰>
class StompClient(
    private val tokenStorage: TokenStorage,
    private val wsUrl: String
) {
    private val krossbowClient = KrossbowStompClient(
        webSocketClient = KtorWebSocketClient(
            HttpClient {
                install(WebSockets)
            }
        )
    )

    private fun connectHeaders(): Map<String, String> {
        val token = tokenStorage.accessToken() ?: tokenStorage.guestToken

        return if (token != null) {
            mapOf("Authorization" to "Bearer $token")
        } else {
            emptyMap()
        }
    }

    suspend fun connect(): StompSession {
        return krossbowClient.connect(
            url = wsUrl,
            customStompConnectHeaders = connectHeaders()
        )
    }
}
