package org.sesacteamproject.passmate.ui.mypage

sealed interface SettingsAction {
    data object Enter : SettingsAction
}
