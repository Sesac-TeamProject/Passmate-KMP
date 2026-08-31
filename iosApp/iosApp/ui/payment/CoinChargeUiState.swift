import Shared

// 코인 충전 (M-12-4·M-12-6) — Compose CoinChargeUiState.kt 미러.
// 완료 화면은 별도 라우트가 아니라 isCompleted 전환으로 같은 라우트 안에서 그린다
struct CoinChargeUiState {
    var isLoading: Bool = true

    var hasLoadError: Bool = false

    var balance: Int = 0

    var presets: [Int] = []

    var selectedAmount: Int = defaultAmount

    var selectedMethod: PaymentMethod = .kakaoPay

    var isProcessing: Bool = false

    var checkout: PortOneRequest? = nil

    var isCompleted: Bool = false

    // 완료 화면 표기용 — 방금 충전한 금액(원 = C)
    var chargedAmount: Int = 0

    var errorMessage: String? = nil

    var isPortOneVisible: Bool {
        checkout != nil
    }
}

// 시안 M-12-4의 기본 선택 금액
private let defaultAmount = 10_000
