package org.sesacteamproject.passmate.core.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// Swift는 Kotlin Flow를 직접 collect할 수 없어 콜백 브리지로 노출한다 (Swift VM 전용)
class SessionEventStreamWatcher(
    private val stream: SessionEventStream
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var job: Job? = null

    fun start(roomId: Long, onEvent: (SessionEventStream.StreamEvent) -> Unit) {
        stop()
        job = scope.launch {
            stream.events(roomId).collect { event ->
                onEvent(event)
            }
        }
    }

    // 호스트 리모컨(M-T2) 전용 — 호스트 토픽 포함 구독. ObjC는 Kotlin 기본인자를 못 쓰므로 별도 메소드로 노출
    fun startAsHost(roomId: Long, onEvent: (SessionEventStream.StreamEvent) -> Unit) {
        stop()
        job = scope.launch {
            stream.events(roomId, isHost = true).collect { event ->
                onEvent(event)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
