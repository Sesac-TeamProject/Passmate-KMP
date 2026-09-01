package org.sesacteamproject.passmate.ui.auth

sealed interface SignInAction {

    data object ClickGoogleSignIn : SignInAction

    // Apple 로그인은 iOS에만 노출한다(팀 결정 2026-09-01) — Android·Desktop에는 버튼이 없다.
    // iosApp 미러와 Action 1:1을 유지하기 위해 액션 자체는 남긴다(규칙 §14).
    data object ClickAppleSignIn : SignInAction

    data object ClickGuestEnter : SignInAction

    data class ReceiveOAuthCallback(
        val accessToken: String,
        val refreshToken: String
    ) : SignInAction
}
