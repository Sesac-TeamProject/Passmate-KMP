package org.sesacteamproject.passmate.ui.mypage

sealed interface EditProfileAction {

    data object Enter : EditProfileAction

    data object Retry : EditProfileAction

    data class ChangeNickname(val text: String) : EditProfileAction

    data object Submit : EditProfileAction
}
