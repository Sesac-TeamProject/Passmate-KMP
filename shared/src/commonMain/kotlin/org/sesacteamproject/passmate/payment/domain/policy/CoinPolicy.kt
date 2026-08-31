package org.sesacteamproject.passmate.payment.domain.policy

// 코인 계산 정책 — 부족분·충전 프리셋 (계약 §결제, 유료 방 입장 M-01 v2 "보유 1,200 C · 부족 8,800 C").
// 최종 차감 판정은 서버(entry-payments 402)가 하며, 여기 계산은 UX용이다.
class CoinPolicy {

    // 프리셋 충전 금액(1 C = ₩1) — 충전 화면 M-12-4의 2×2 그리드와 같은 목록.
    // 부족분보다 큰 첫 프리셋 또는 부족분 자체를 제안한다.
    val presets: List<Int> = listOf(5_000, 10_000, 30_000, 50_000)

    // 참가비 대비 부족 코인(0이면 충전 불필요)
    fun shortfall(balance: Int, entryFee: Int): Int {
        val diff = entryFee - balance

        return if (diff > 0) {
            diff
        } else {
            0
        }
    }

    fun hasEnough(balance: Int, entryFee: Int): Boolean {
        return balance >= entryFee
    }

    // 부족분을 덮는 최소 충전 금액 — 부족분 이상인 첫 프리셋, 모든 프리셋보다 크면 부족분 자체
    fun suggestedChargeAmount(shortfall: Int): Int {
        return presets.firstOrNull { it >= shortfall } ?: shortfall
    }
}
