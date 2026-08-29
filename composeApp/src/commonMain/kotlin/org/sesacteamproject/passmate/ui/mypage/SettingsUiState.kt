package org.sesacteamproject.passmate.ui.mypage

import org.sesacteamproject.passmate.user.domain.model.UserProfile

data class SettingsUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val profile: UserProfile? = null,
    // 로그아웃·탈퇴 요청 in-flight — 중복 호출 방지 (규칙 §9)
    val isProcessing: Boolean = false
)
