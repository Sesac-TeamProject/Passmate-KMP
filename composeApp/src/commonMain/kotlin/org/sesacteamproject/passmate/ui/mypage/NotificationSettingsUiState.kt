package org.sesacteamproject.passmate.ui.mypage

data class NotificationSettingsUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val sessionStart: Boolean = true,
    val ratingRequest: Boolean = true,
    val settlementDone: Boolean = true,
    // 저장 in-flight — 토글 즉시 저장 방식이라 저장 중 추가 토글을 막는다 (규칙 §9)
    val isSaving: Boolean = false
)
