package org.sesacteamproject.passmate.auth.data.repository

import org.sesacteamproject.passmate.auth.data.dto.DevLoginRequest
import org.sesacteamproject.passmate.auth.data.dto.DevLoginResponse
import org.sesacteamproject.passmate.auth.data.remote.AuthRemoteDataSource
import org.sesacteamproject.passmate.auth.domain.repository.AuthRepository
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.network.ApiClient
import org.sesacteamproject.passmate.core.network.apiCall
import org.sesacteamproject.passmate.core.network.isLocalDevServer
import org.sesacteamproject.passmate.core.storage.TokenStorage

class AuthRepositoryImpl(
    private val apiClient: ApiClient,
    private val tokenStorage: TokenStorage,
    private val remoteDataSource: AuthRemoteDataSource
) : AuthRepository {

    // dev-login 응답의 토큰 쌍을 딥링크 콜백과 같은 경로로 저장한다
    private suspend fun AppResult<DevLoginResponse>.flatMapToSession(): AppResult<Unit> {
        return when (this) {
            is AppResult.Success -> completeSignIn(value.accessToken, value.refreshToken)
            is AppResult.Failure -> AppResult.Failure(error)
        }
    }

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

    override fun isDevSignInAvailable(): Boolean {
        return isLocalDevServer(apiClient.baseUrl)
    }

    override suspend fun devSignIn(): AppResult<Unit> {
        val request = DevLoginRequest(DEV_LOGIN_KEY)
        val result = apiCall { remoteDataSource.devLogin(request) }

        return result.flatMapToSession()
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

    companion object {

        // 백엔드와 합의된 개발 계정 키 — 로컬 DB에만 존재한다
        private const val DEV_LOGIN_KEY = "kmp-integration-check"
    }
}
