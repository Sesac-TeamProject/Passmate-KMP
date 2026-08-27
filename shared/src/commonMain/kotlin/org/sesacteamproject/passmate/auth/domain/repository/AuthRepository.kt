package org.sesacteamproject.passmate.auth.domain.repository

import org.sesacteamproject.passmate.core.model.AppResult

interface AuthRepository {

    fun googleSignInUrl(): String

    // 시스템 브라우저 OAuth 후 딥링크(passmate://oauth/callback)로 전달된 토큰 쌍을 저장한다
    suspend fun completeSignIn(accessToken: String, refreshToken: String): AppResult<Unit>

    fun isSignedIn(): Boolean
}
