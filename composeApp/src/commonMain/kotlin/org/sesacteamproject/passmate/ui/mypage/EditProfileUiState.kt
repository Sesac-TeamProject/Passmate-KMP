package org.sesacteamproject.passmate.ui.mypage

data class EditProfileUiState(
    val nickname: String = "",
    val avatarId: Int? = null,
    // 제출 in-flight — 중복 호출 방지 (규칙 §9)
    val isSubmitting: Boolean = false
) {

    val canSubmit: Boolean
        get() = !isSubmitting && nickname.isNotBlank()
}
