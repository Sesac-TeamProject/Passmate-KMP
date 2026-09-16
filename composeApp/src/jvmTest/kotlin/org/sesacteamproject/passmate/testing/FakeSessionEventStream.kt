package org.sesacteamproject.passmate.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import org.sesacteamproject.passmate.core.network.SessionEventStream
import org.sesacteamproject.passmate.core.network.StompClient
import org.sesacteamproject.passmate.core.storage.TokenStorage

// 테스트가 Connected/Disconnected/Received를 직접 흘려 넣는 가짜 스트림.
// 실제 STOMP 연결은 하지 않는다 — StompClient는 부모 생성자 요구 때문에 만들 뿐 접속하지 않는다
class FakeSessionEventStream : SessionEventStream(StompClient(TokenStorage(), "ws://localhost/ws")) {

    private val events = MutableSharedFlow<StreamEvent>()

    var subscribeCount: Int = 0

    // 지금 살아 있는 수집기 수. 구독 "횟수"만 세면 이전 구독을 끊지 않아도 통과하므로
    // 재구독이 정말 갈아치우는지는 이 값으로 본다
    var activeCollectorCount: Int = 0

    override fun events(roomId: Long, isHost: Boolean): Flow<StreamEvent> {
        subscribeCount += 1
        return events
            .onStart { activeCollectorCount += 1 }
            .onCompletion { activeCollectorCount -= 1 }
    }

    suspend fun emit(event: StreamEvent) {
        events.emit(event)
    }
}
