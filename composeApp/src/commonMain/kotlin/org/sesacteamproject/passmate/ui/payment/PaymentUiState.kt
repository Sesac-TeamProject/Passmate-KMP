package org.sesacteamproject.passmate.ui.payment

import org.sesacteamproject.passmate.component.PortOneRequest
import org.sesacteamproject.passmate.payment.domain.model.PaymentMethod
import org.sesacteamproject.passmate.room.domain.model.RoomInfo

// 유료 방 입장 결제 화면 상태 (M-01 v2 / W-11). 보유 코인·참가비·부족분·결제 수단.
data class PaymentUiState(
    val isLoading: Boolean = true,
    val hasLoadError: Boolean = false,
    val room: RoomInfo? = null,
    val balance: Int = 0,
    val shortfall: Int = 0,
    val nickname: String = "",
    val avatarId: Int = 1,
    val selectedMethod: PaymentMethod = PaymentMethod.KAKAO_PAY,
    val isProcessing: Boolean = false,
    // 코인이 모자랄 때 뜨는 M-11 시트. 표시 여부 판단은 VM, 시트 생명주기는 화면이 갖는다 (규칙 §11-1)
    val isCoinShortageSheetVisible: Boolean = false,
    val checkout: PortOneRequest? = null,
    val errorMessage: String? = null
) {
    val entryFee: Int
        get() = room?.entryFee ?: 0

    val hasEnough: Boolean
        get() = shortfall <= 0

    val isPortOneVisible: Boolean
        get() = checkout != null
}
