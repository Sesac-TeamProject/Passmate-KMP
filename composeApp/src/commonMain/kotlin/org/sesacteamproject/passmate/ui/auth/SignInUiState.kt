package org.sesacteamproject.passmate.ui.auth

data class SignInUiState(
    val isSigningIn: Boolean = false,
    // 로컬 개발 서버에 붙어 있을 때만 개발용 로그인 진입점을 그린다 (운영 URL이면 false)
    val isDevSignInAvailable: Boolean = false
)
