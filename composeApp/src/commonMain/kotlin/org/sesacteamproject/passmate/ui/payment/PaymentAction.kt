package org.sesacteamproject.passmate.ui.payment

import org.sesacteamproject.passmate.component.PortOneResult

sealed interface PaymentAction {

    // 진입 시 pin을 전달받아 방 정보·보유 코인을 로드한다 (Waiting/Play와 동일한 인자 전달 패턴)
    data class Start(val pin: String) : PaymentAction

    data class ChangeNickname(val nickname: String) : PaymentAction

    data class SelectAvatar(val avatarId: Int) : PaymentAction

    // 주 CTA — 잔액 충분하면 참가비 차감, 부족하면 코인 부족 시트(M-11)를 띄운다
    data object ClickPay : PaymentAction

    // 코인 부족 시트의 충전 CTA — 바로 포트원 결제창이 뜬다 (수단은 결제창에서 고른다)
    data object ConfirmCharge : PaymentAction

    data object DismissCoinShortage : PaymentAction

    data class ReceivePortOneResult(val result: PortOneResult) : PaymentAction

    data object DismissError : PaymentAction

    data object Retry : PaymentAction
}
