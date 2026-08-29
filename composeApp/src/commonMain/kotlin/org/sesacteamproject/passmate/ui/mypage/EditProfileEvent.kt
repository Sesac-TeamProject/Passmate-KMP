package org.sesacteamproject.passmate.ui.mypage

sealed interface EditProfileEvent {

    data object Saved : EditProfileEvent

    data class ShowNotice(val message: String) : EditProfileEvent
}
