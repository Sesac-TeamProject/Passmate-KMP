package org.sesacteamproject.passmate.ui.mypage

data class CharacterEditUiState(
    val avatarId: Int? = null,
    val isLoading: Boolean = true,
    val hasLoadError: Boolean = false,
    // 제출 in-flight — 중복 호출 방지 (규칙 §9)
    val isSubmitting: Boolean = false
) {

    val canSubmit: Boolean
        get() = !isLoading && !isSubmitting && avatarId != null
}
