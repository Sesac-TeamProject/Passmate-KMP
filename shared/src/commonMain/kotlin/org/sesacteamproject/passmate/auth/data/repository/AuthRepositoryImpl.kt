package org.sesacteamproject.passmate.auth.data.repository

import org.sesacteamproject.passmate.auth.domain.repository.AuthRepository
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.network.ApiClient
import org.sesacteamproject.passmate.core.storage.TokenStorage

class AuthRepositoryImpl(
    private val apiClient: ApiClient,
    private val tokenStorage: TokenStorage
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
}
