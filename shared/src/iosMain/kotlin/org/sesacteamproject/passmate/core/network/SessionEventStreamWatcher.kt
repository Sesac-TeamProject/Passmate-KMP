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

    fun stop() {
        job?.cancel()
        job = null
    }
}
