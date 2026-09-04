import Shared

// 유료 방 입장 결제 (M-01 v2 / W-11) — Compose PaymentUiState.kt 미러
struct PaymentUiState {
    var isLoading: Bool = true

    var hasLoadError: Bool = false

    var room: RoomInfo? = nil

    var balance: Int = 0

    var shortfall: Int = 0

    var nickname: String = ""

    var avatarId: Int = 1

    var selectedMethod: PaymentMethod = .kakaoPay

    var isProcessing: Bool = false

    // 코인이 모자랄 때 뜨는 M-11 시트. 표시 여부 판단은 VM, 시트 생명주기는 화면이 갖는다 (규칙 §11-1)
    var isCoinShortageSheetVisible: Bool = false

    var checkout: PortOneRequest? = nil

    var errorMessage: String? = nil

    var entryFee: Int {
        Int(room?.entryFee?.int32Value ?? 0)
    }

    var hasEnough: Bool {
        shortfall <= 0
    }

    var isPortOneVisible: Bool {
        checkout != nil
    }
}
