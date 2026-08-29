package org.sesacteamproject.passmate.auth.domain.repository

import org.sesacteamproject.passmate.core.model.AppResult

interface AuthRepository {

    fun googleSignInUrl(): String

    // 시스템 브라우저 OAuth 후 딥링크(passmate://oauth/callback)로 전달된 토큰 쌍을 저장한다
    suspend fun completeSignIn(accessToken: String, refreshToken: String): AppResult<Unit>

    fun isSignedIn(): Boolean

    // 로그아웃 (M-12-11) — POST /auth/logout 최선 시도 후 로컬 토큰 정리(항상 성공)
    suspend fun signOut(): AppResult<Unit>

    // 로컬 세션만 정리 — 회원 탈퇴(DELETE /users/me) 성공 후 호출 (M-12-12)
    fun clearSession()
}
