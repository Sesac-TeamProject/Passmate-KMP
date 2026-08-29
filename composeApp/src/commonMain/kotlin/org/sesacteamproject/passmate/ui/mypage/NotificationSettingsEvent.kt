package org.sesacteamproject.passmate.ui.mypage

sealed interface NotificationSettingsEvent {

    data class ShowNotice(val message: String) : NotificationSettingsEvent
}
