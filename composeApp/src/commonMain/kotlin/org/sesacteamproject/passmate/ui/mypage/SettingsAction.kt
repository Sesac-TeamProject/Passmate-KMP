package org.sesacteamproject.passmate.ui.mypage

sealed interface SettingsAction {

    data object Enter : SettingsAction

    data object Retry : SettingsAction

    // 계정 정보·캐릭터 변경 시트 (M-12-1·M-12-7)
    data object ClickEditProfile : SettingsAction

    data object ClickPaymentMethod : SettingsAction

    data object ClickNotifications : SettingsAction

    data object ClickCoinHistory : SettingsAction

    // 확인 다이얼로그를 거친 뒤 호출된다 — 다이얼로그 소유는 화면 (규칙 §11-1)
    data object ConfirmSignOut : SettingsAction

    data object ConfirmDeleteAccount : SettingsAction

    // 시트에서 프로필 저장 완료 — 카드 갱신
    data object ProfileUpdated : SettingsAction

    data class Notice(val message: String) : SettingsAction
}
