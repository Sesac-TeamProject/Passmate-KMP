package org.sesacteamproject.passmate.ui.mypage

sealed interface SettingsAction {
    data object Enter : SettingsAction

    // 확인 다이얼로그를 거친 뒤 호출된다 — 다이얼로그 소유는 화면 (규칙 §11-1)
    data object ConfirmDeleteAccount : SettingsAction
}
