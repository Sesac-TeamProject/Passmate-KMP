package org.sesacteamproject.passmate.ui.mypage

import org.sesacteamproject.passmate.payment.domain.model.CoinTransaction
import org.sesacteamproject.passmate.payment.domain.model.NextPayout
import org.sesacteamproject.passmate.payment.domain.model.SettlementAccountSummary
import org.sesacteamproject.passmate.room.domain.model.HostLevel
import org.sesacteamproject.passmate.user.domain.model.UserProfile

// 마이 탭 루트 (M-12). 프로필 실패 = 전체 에러, 코인·정산 실패 = 해당 카드만 실패 표시 (규칙 §9)
data class MyInfoUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val profile: UserProfile? = null,
    // 명성 등급 — 서버 /users/me가 등급을 주지 않아 /users/me/grade로 따로 받는다 (시안 M-12 닉네임 옆 배지)
    val level: HostLevel? = null,
    val recentTransaction: CoinTransaction? = null,
    val isCoinInfoFailed: Boolean = false,
    // 코인 카드 재시도 in-flight — 중복 요청 방지 (규칙 §9)
    val isCoinInfoLoading: Boolean = false,
    val settlementAccount: SettlementAccountSummary? = null,
    val nextPayout: NextPayout? = null,
    val isEarningsFailed: Boolean = false,
    // 정산 카드 재시도 in-flight — 중복 요청 방지 (규칙 §9)
    val isEarningsLoading: Boolean = false,
    // 로그아웃 요청 in-flight — 중복 호출 방지 (규칙 §9)
    val isProcessing: Boolean = false
) {
    // 카드 하나라도 실패하면 상단 안내 배너를 띄운다 (시안 M-12e)
    val hasPartialFailure: Boolean
        get() = isCoinInfoFailed || isEarningsFailed
}
