package org.sesacteamproject.passmate.ui.home

sealed interface RoomListEvent {

    // 방 선택 → 입장 화면으로 이동(무료·유료 분기는 Join이 방 정보로 판단한다)
    data class OpenRoom(val pin: String) : RoomListEvent

    data object OpenPinEntry : RoomListEvent

    // 선생님 프로필 시트 열기 (M-10) — 시트 표시는 화면이 소유한다 (규칙 §11-1)
    data class OpenHostProfile(val hostId: Long) : RoomListEvent

    data class ShowNotice(val message: String) : RoomListEvent
}
