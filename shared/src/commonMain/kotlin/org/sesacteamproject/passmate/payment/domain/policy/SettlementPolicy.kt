package org.sesacteamproject.passmate.payment.domain.policy

// 정산 비율 단일 출처 — 기획서 §13.3 고정값(8:2, FR-056).
// 서버가 비율을 내려주지 않으므로 앱이 값을 들고 있다. 화면마다 숫자를 적으면
// 비율이 바뀔 때 결제 화면과 정산 화면이 서로 다른 수치를 말하게 된다.
// 금액 분해 자체는 서버 권위라 클라이언트는 비율 안내만 한다 (규칙 §13).
object SettlementPolicy {

    val hostSharePercent: Int = 80

    val platformFeePercent: Int
        get() = 100 - hostSharePercent
}
