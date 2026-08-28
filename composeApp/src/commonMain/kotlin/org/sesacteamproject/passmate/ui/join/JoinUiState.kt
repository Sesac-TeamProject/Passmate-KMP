package org.sesacteamproject.passmate.ui.join

import org.sesacteamproject.passmate.component.StudentAvatars

data class JoinUiState(
    val pin: String = "",
    val nickname: String = "",
    val avatarId: Int = StudentAvatars.DEFAULT_ID,
    val isJoining: Boolean = false,
    val isSignedIn: Boolean = false
)
