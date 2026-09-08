package org.sesacteamproject.passmate.ui.auth

sealed interface SignInEvent {

    // 플랫폼 구글 로그인 시트를 띄워 달라는 요청 — ID 토큰은 액션으로 되돌아온다
    data object RequestGoogleSignIn : SignInEvent

    data object SignInCompleted : SignInEvent

    data object GuestEnterRequested : SignInEvent

    data class ShowNotice(val message: String) : SignInEvent
}
