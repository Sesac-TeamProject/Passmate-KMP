package org.sesacteamproject.passmate.ui.mypage

sealed interface CharacterEditAction {

    data object Enter : CharacterEditAction

    data object Retry : CharacterEditAction

    data class SelectAvatar(val avatarId: Int) : CharacterEditAction

    data object Submit : CharacterEditAction
}
