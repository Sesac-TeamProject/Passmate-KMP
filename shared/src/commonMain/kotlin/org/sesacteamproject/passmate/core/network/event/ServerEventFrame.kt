package org.sesacteamproject.passmate.core.network.event

// ts는 재접속 스냅샷 프로토콜의 "스냅샷 ts 이전 이벤트 폐기" 판정에 사용한다 (규칙 §2-1-2)
data class ServerEventFrame(
    val ts: String,
    val event: ServerEvent
)
