package org.sesacteamproject.passmate.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.subscribeText
import org.sesacteamproject.passmate.core.network.event.ServerEventDecoder
import org.sesacteamproject.passmate.core.network.event.ServerEventFrame

// STOMP 수신을 단일 Flow로 일원화한다 (규칙 §9). 재연결 시 Connected를 발행해
// 세션 기능 계층이 스냅샷 조회(GET /rooms/{pin}/session/snapshot)를 트리거하게 한다.
class SessionEventStream(
    private val stompClient: StompClient
) {
    sealed interface StreamEvent {

        data object Connected : StreamEvent

        data object Disconnected : StreamEvent

        data class Received(val frame: ServerEventFrame) : StreamEvent
    }

    fun events(pin: String): Flow<StreamEvent> {
        return channelFlow {
            var attempt = 0

            while (isActive) {
                var session: StompSession? = null

                try {
                    session = stompClient.connect()
                    attempt = 0
                    send(StreamEvent.Connected)
                    coroutineScope {
                        launch { collectDestination(session, "/topic/rooms/$pin") }
                        launch { collectDestination(session, "/user/queue/feedback") }
                        launch { collectDestination(session, "/user/queue/errors") }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    send(StreamEvent.Disconnected)
                } finally {
                    try {
                        session?.disconnect()
                    } catch (e: Exception) {
                        // 이미 끊긴 연결 정리 실패는 무시
                    }
                }
                attempt += 1
                delay(minOf(RETRY_BASE_DELAY_MS * attempt, RETRY_MAX_DELAY_MS))
            }
        }
    }

    private suspend fun kotlinx.coroutines.channels.ProducerScope<StreamEvent>.collectDestination(
        session: StompSession,
        destination: String
    ) {
        session.subscribeText(destination).collect { text ->
            val frame = ServerEventDecoder.decode(text)

            if (frame != null) {
                send(StreamEvent.Received(frame))
            }
        }
    }

    companion object {
        private const val RETRY_BASE_DELAY_MS = 1_000L
        private const val RETRY_MAX_DELAY_MS = 5_000L
    }
}
