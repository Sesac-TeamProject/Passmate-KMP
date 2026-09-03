package org.sesacteamproject.passmate.ui.profile

import org.sesacteamproject.passmate.user.domain.model.ReportReason

sealed interface HostProfileAction {

    data class Enter(val hostId: Long) : HostProfileAction

    data class Retry(val hostId: Long) : HostProfileAction

    // 프로필의 방 목록도 `PublicRoomResponse`라 pin이 없다 — roomId로 받는다
    data class ClickRoom(val roomId: Long) : HostProfileAction

    data object ClickBlock : HostProfileAction

    data class SubmitReport(val reason: ReportReason) : HostProfileAction
}
