package org.sesacteamproject.passmate.ui.mypage

sealed interface MyInfoAction {
    data object Enter : MyInfoAction

    data object Retry : MyInfoAction

    // 프로필 카드 탭 → 내 명성·뱃지 (M-09)
    data object ClickProfile : MyInfoAction

    // 닉네임·내 캐릭터 변경 시트 (M-12-1·M-12-7)
    data object ClickEditProfile : MyInfoAction

    // 코인 충전 화면 (M-12-4·M-12-6)
    data object ClickCharge : MyInfoAction

    data object ClickPaymentMethod : MyInfoAction

    data object ClickCoinHistory : MyInfoAction

    data object ClickSettlementAccount : MyInfoAction

    data object ClickEarnings : MyInfoAction

    data object ClickNotifications : MyInfoAction

    data object ClickSettings : MyInfoAction

    // 확인 다이얼로그를 거친 뒤 호출된다 — 다이얼로그 소유는 화면 (규칙 §11-1)
    data object ConfirmSignOut : MyInfoAction

    // 시트 저장 완료 — 해당 섹션만 다시 불러온다
    data object ProfileUpdated : MyInfoAction

    data object PaymentMethodUpdated : MyInfoAction

    data object AccountUpdated : MyInfoAction

    data class Notice(val message: String) : MyInfoAction
}
