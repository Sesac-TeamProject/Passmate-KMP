package org.sesacteamproject.passmate.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.sesacteamproject.passmate.core.network.SessionEventStream
import org.sesacteamproject.passmate.core.network.StompClient
import org.sesacteamproject.passmate.core.storage.TokenStorage

// 테스트가 Connected/Disconnected/Received를 직접 흘려 넣는 가짜 스트림.
// 실제 STOMP 연결은 하지 않는다 — StompClient는 부모 생성자 요구 때문에 만들 뿐 접속하지 않는다
class FakeSessionEventStream : SessionEventStream(StompClient(TokenStorage(), "ws://localhost/ws")) {

    private val events = MutableSharedFlow<StreamEvent>()

    var subscribeCount: Int = 0

    override fun events(roomId: Long, isHost: Boolean): Flow<StreamEvent> {
        subscribeCount += 1
        return events
    }

    suspend fun emit(event: StreamEvent) {
        events.emit(event)
    }
}
