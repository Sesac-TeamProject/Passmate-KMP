package org.sesacteamproject.passmate.ui.profile

import org.sesacteamproject.passmate.user.domain.model.HostProfile

data class HostProfileUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val profile: HostProfile? = null,
    // 제출 in-flight — 중복 호출 방지 (규칙 §9)
    val isSubmitting: Boolean = false,
    val isReported: Boolean = false
)
