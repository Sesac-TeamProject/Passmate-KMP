package org.sesacteamproject.passmate.ui.join

sealed interface JoinAction {

    data class ChangePin(val pin: String) : JoinAction

    data class ChangeNickname(val nickname: String) : JoinAction

    data class SelectAvatar(val avatarId: Int) : JoinAction

    data object ClickScanQr : JoinAction

    data class ReceiveQrResult(val text: String?) : JoinAction

    data object ClickJoin : JoinAction

    data object ClickSignIn : JoinAction
}
