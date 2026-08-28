package org.sesacteamproject.passmate.ui.join

import org.sesacteamproject.passmate.component.StudentAvatars
import org.sesacteamproject.passmate.room.domain.model.RoomInfo

data class JoinUiState(
    val pin: String = "",
    val nickname: String = "",
    val avatarId: Int = StudentAvatars.DEFAULT_ID,
    val isJoining: Boolean = false,
    val isSignedIn: Boolean = false,
    // 입장 전 방 정보(호스트 등급·별점) — PIN 6자리 입력 시 프리페치 (T081)
    val roomInfo: RoomInfo? = null,
    val isLoadingRoomInfo: Boolean = false
)
