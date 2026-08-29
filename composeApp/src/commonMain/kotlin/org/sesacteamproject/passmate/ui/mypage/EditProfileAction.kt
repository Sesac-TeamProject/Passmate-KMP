package org.sesacteamproject.passmate.ui.mypage

sealed interface EditProfileAction {

    data class Enter(val nickname: String, val avatarId: Int?) : EditProfileAction

    data class ChangeNickname(val text: String) : EditProfileAction

    data class SelectAvatar(val avatarId: Int) : EditProfileAction

    data object Submit : EditProfileAction
}
