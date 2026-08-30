package org.sesacteamproject.passmate.ui.mypage

sealed interface MyInfoAction {

    data object Enter : MyInfoAction

    data object Retry : MyInfoAction

    // 계정 정보·캐릭터 변경 시트 (M-12-1·M-12-7)
    data object ClickEditProfile : MyInfoAction

    data object ClickPaymentMethod : MyInfoAction

    data object ClickNotifications : MyInfoAction

    data object ClickCoinHistory : MyInfoAction

    // 확인 다이얼로그를 거친 뒤 호출된다 — 다이얼로그 소유는 화면 (규칙 §11-1)
    data object ConfirmSignOut : MyInfoAction

    data object ConfirmDeleteAccount : MyInfoAction

    // 시트에서 프로필 저장 완료 — 카드 갱신
    data object ProfileUpdated : MyInfoAction

    data class Notice(val message: String) : MyInfoAction
}
