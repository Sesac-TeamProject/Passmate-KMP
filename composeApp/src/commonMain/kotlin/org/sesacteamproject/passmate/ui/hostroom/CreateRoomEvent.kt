package org.sesacteamproject.passmate.ui.hostroom

sealed interface CreateRoomEvent {

    // 방 생성 완료 — PIN은 서버가 자동 발급 (FR-004)
    data class Created(val pin: String) : CreateRoomEvent

    data class ShowNotice(val message: String) : CreateRoomEvent
}
