import Foundation
import Shared

// 마이 탭 루트 (M-12). 프로필 실패 = 전체 에러, 코인·정산 실패 = 해당 카드만 (규칙 §9)
struct MyInfoUiState {
    var isLoading: Bool = true

    var loadFailed: Bool = false

    var profile: UserProfile?

    var defaultMethod: PaymentMethod?

    var recentTransaction: CoinTransaction?

    var isCoinInfoFailed: Bool = false

    var settlementAccount: SettlementAccountSummary?

    var nextPayout: NextPayout?

    var isEarningsFailed: Bool = false

    // 로그아웃 요청 in-flight — 중복 호출 방지 (규칙 §9)
    var isProcessing: Bool = false

    // 카드 하나라도 실패하면 상단 안내 배너를 띄운다 (시안 M-12e)
    var hasPartialFailure: Bool {
        return isCoinInfoFailed || isEarningsFailed
    }
}
