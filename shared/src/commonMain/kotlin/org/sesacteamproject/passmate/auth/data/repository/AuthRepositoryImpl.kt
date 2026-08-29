package org.sesacteamproject.passmate.auth.data.repository

import org.sesacteamproject.passmate.auth.data.remote.AuthRemoteDataSource
import org.sesacteamproject.passmate.auth.domain.repository.AuthRepository
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.network.ApiClient
import org.sesacteamproject.passmate.core.storage.TokenStorage

class AuthRepositoryImpl(
    private val apiClient: ApiClient,
    private val tokenStorage: TokenStorage,
    private val remoteDataSource: AuthRemoteDataSource
) : AuthRepository {

    override fun googleSignInUrl(): String {
        return "${apiClient.baseUrl}/auth/oauth/google?client=mobile"
    }

    override suspend fun completeSignIn(accessToken: String, refreshToken: String): AppResult<Unit> {
        return if (accessToken.isBlank() || refreshToken.isBlank()) {
            AppResult.Failure(AppError.ValidationFailed(serverMessage = "로그인 콜백 토큰이 비어 있습니다"))
        } else {
            tokenStorage.saveMemberTokens(accessToken, refreshToken)
            AppResult.Success(Unit)
        }
    }

    override fun isSignedIn(): Boolean {
        return tokenStorage.accessToken() != null
    }

    override suspend fun signOut(): AppResult<Unit> {
        // 서버 refresh 무효화는 최선 시도 — 네트워크 실패여도 로컬 로그아웃은 진행한다 (M-12-11)
        try {
            remoteDataSource.logout()
        } catch (e: Exception) {
            // 무효화 실패는 무시 — refresh 만료로 자연 소멸
        }
        clearSession()
        return AppResult.Success(Unit)
    }

    override fun clearSession() {
        tokenStorage.clearMemberTokens()
        tokenStorage.guestToken = null
    }
}
