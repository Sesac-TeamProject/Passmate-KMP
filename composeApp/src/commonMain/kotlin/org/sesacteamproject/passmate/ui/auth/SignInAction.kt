package org.sesacteamproject.passmate.ui.auth

sealed interface SignInAction {

    data object ClickGoogleSignIn : SignInAction

    // Apple 로그인은 iOS에만 노출한다(팀 결정 2026-09-01) — Android·Desktop에는 버튼이 없다.
    // iosApp 미러와 Action 1:1을 유지하기 위해 액션 자체는 남긴다(규칙 §14).
    data object ClickAppleSignIn : SignInAction

    data object ClickGuestEnter : SignInAction

    // 개발용 로그인 (POST /auth/dev-login) — 로컬 개발 서버에서만 노출된다
    data object ClickDevSignIn : SignInAction

    // 네이티브 SDK가 Google ID 토큰을 돌려줬다 — 서버 로그인은 여기서 시작한다
    data class ReceiveGoogleIdToken(val idToken: String) : SignInAction

    // 사용자가 구글 시트를 닫았다 — 실패가 아니므로 안내를 띄우지 않는다
    data object CancelGoogleSignIn : SignInAction

    // 구글 SDK가 토큰을 주지 못했다 — 안내 문구는 ViewModel이 정한다 (규칙 §7)
    data object FailGoogleSignIn : SignInAction
}
