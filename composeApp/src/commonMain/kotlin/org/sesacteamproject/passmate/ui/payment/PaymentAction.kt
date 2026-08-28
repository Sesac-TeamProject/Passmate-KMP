package org.sesacteamproject.passmate.ui.payment

import org.sesacteamproject.passmate.component.PortOneResult
import org.sesacteamproject.passmate.payment.domain.model.PaymentMethod

sealed interface PaymentAction {

    // 진입 시 pin을 전달받아 방 정보·보유 코인을 로드한다 (Waiting/Play와 동일한 인자 전달 패턴)
    data class Start(val pin: String) : PaymentAction

    data class ChangeNickname(val nickname: String) : PaymentAction

    data class SelectAvatar(val avatarId: Int) : PaymentAction

    data class SelectMethod(val method: PaymentMethod) : PaymentAction

    // 주 CTA — 잔액 충분하면 참가비 차감, 부족하면 충전(포트원) 시작
    data object ClickPay : PaymentAction

    data class ReceivePortOneResult(val result: PortOneResult) : PaymentAction

    data object DismissError : PaymentAction

    data object Retry : PaymentAction
}
