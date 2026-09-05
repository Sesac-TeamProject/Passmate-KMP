package org.sesacteamproject.passmate.ui.mypage

sealed interface MyInfoAction {
    data object Enter : MyInfoAction

    data object Retry : MyInfoAction

    // 카드 단위 재시도 — 코인·정산은 프로필과 독립 로드다 (규칙 §9, 시안 M-12e)
    data object RetryCoinInfo : MyInfoAction

    data object RetryEarnings : MyInfoAction

    // 프로필 카드 탭 → 내 명성·뱃지 (M-09)
    data object ClickProfile : MyInfoAction

    // 계정 정보 변경 페이지 (M-12-1) — 캐릭터는 그 안에서 M-12-7로 간다
    data object ClickEditProfile : MyInfoAction

    // 코인 충전 화면 (M-12-4·M-12-6)
    data object ClickCharge : MyInfoAction

    data object ClickPaymentMethod : MyInfoAction

    data object ClickCoinHistory : MyInfoAction

    data object ClickSettlementAccount : MyInfoAction

    data object ClickEarnings : MyInfoAction

    data object ClickNotifications : MyInfoAction

    // card/기타 회원 탈퇴 행 → 전용 화면 push (M-12-12)
    data object ClickDeleteAccount : MyInfoAction

    // card/기타 약관 · 개인정보 처리방침 행 — 전용 화면이 아직 없어 안내만 한다
    data object ClickTerms : MyInfoAction

    // 확인 다이얼로그를 거친 뒤 호출된다 — 다이얼로그 소유는 화면 (규칙 §11-1)
    data object ConfirmSignOut : MyInfoAction

    data class Notice(val message: String) : MyInfoAction
}
