package org.sesacteamproject.passmate.ui.mypage

data class EditProfileUiState(
    val nickname: String = "",
    // 이메일은 로그인 ID라 표시만 하고 바꾸지 않는다 (시안 M-12-1)
    val email: String? = null,
    // 캐릭터는 M-12-7에서 바꾼다 — 여기서는 현재 값을 보여 주기만 한다
    val avatarId: Int? = null,
    val isLoading: Boolean = true,
    val hasLoadError: Boolean = false,
    // 제출 in-flight — 중복 호출 방지 (규칙 §9)
    val isSubmitting: Boolean = false
) {

    val canSubmit: Boolean
        get() = !isLoading && !isSubmitting && nickname.isNotBlank()
}
