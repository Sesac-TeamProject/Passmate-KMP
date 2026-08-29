package org.sesacteamproject.passmate.ui.mypage

// 알림 항목 3종 (M-12-10) — 세션 시작·별점 요청·정산 완료
enum class NotificationKind {
    SESSION_START,
    RATING_REQUEST,
    SETTLEMENT_DONE
}

sealed interface NotificationSettingsAction {

    data object Enter : NotificationSettingsAction

    data object Retry : NotificationSettingsAction

    // 토글 즉시 저장 — 실패 시 원복
    data class Toggle(val kind: NotificationKind) : NotificationSettingsAction
}
