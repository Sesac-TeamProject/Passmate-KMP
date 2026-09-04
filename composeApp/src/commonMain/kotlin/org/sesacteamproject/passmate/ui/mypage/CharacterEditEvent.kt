package org.sesacteamproject.passmate.ui.mypage

sealed interface CharacterEditEvent {

    data object Saved : CharacterEditEvent

    data class ShowNotice(val message: String) : CharacterEditEvent
}
