package org.sesacteamproject.passmate.ui.payment

sealed interface EarningsAction {

    data object Enter : EarningsAction

    data object Retry : EarningsAction

    data object LoadMore : EarningsAction

    // 결제 · 정산 내역 헤더의 "전체 보기 ›" — 코인·결제 내역 전체 목록으로 이동
    data object ClickViewAllHistory : EarningsAction

    // 계좌 관리 → 정산 계좌 시트 (M-12-3, 시트 표시는 화면이 소유).
    // 빈 상태 「계좌 등록하기」 CTA도 같은 시트를 열어 같은 액션을 쓴다
    data object ClickManageAccount : EarningsAction

    // 빈 상태 「유료 방 만들기」 CTA — 방 개설은 「내가 만든 방」 탭의 새 방 만들기 시트(M-13)가 담당한다
    data object ClickCreatePaidRoom : EarningsAction

    // 시트에서 계좌 저장 완료 — 하단 계좌 요약 갱신
    data object AccountSaved : EarningsAction

    data class Notice(val message: String) : EarningsAction
}
