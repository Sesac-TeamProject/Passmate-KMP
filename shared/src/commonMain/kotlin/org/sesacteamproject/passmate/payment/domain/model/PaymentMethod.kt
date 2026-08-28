package org.sesacteamproject.passmate.payment.domain.model

// 코인 충전 결제 수단 (계약 §결제 — method ∈ {KAKAO_PAY, NAVER_PAY, TOSS_PAY, CARD, TRANSFER})
// wireValue는 서버 전송값, label은 화면 표기. 카드 정보는 저장하지 않는다(포트원 처리)
enum class PaymentMethod(val wireValue: String, val label: String) {

    KAKAO_PAY("KAKAO_PAY", "카카오페이"),
    NAVER_PAY("NAVER_PAY", "네이버페이"),
    TOSS_PAY("TOSS_PAY", "토스페이"),
    CARD("CARD", "신용/체크카드"),
    TRANSFER("TRANSFER", "계좌이체");

    companion object {

        fun from(wireValue: String?): PaymentMethod? {
            return entries.firstOrNull { it.wireValue == wireValue }
        }
    }
}
