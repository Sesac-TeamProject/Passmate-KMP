package org.sesacteamproject.passmate.auth.domain.repository

import org.sesacteamproject.passmate.core.model.AppResult

interface AuthRepository {

    // 네이티브 SDK가 받아 온 Google ID 토큰으로 로그인한다 (POST /auth/login/google)
    suspend fun signInWithGoogle(idToken: String): AppResult<Unit>

    // 로컬 개발 서버(dev-login이 있는 서버)에 붙어 있는지 — 운영 URL이면 false
    fun isDevSignInAvailable(): Boolean

    // 개발용 로그인 (POST /auth/dev-login) — Google 로그인이 붙기 전까지의 로컬 대체 경로
    suspend fun devSignIn(): AppResult<Unit>

    fun isSignedIn(): Boolean

    // 로그아웃 (M-12-11) — POST /auth/logout 최선 시도 후 로컬 토큰 정리(항상 성공)
    suspend fun signOut(): AppResult<Unit>

    // 로컬 세션만 정리 — 회원 탈퇴(DELETE /users/me) 성공 후 호출 (M-12-12)
    fun clearSession()
}
