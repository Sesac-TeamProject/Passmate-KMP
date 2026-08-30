package org.sesacteamproject.passmate.testing

import org.sesacteamproject.passmate.auth.domain.repository.AuthRepository
import org.sesacteamproject.passmate.core.model.AppResult

class FakeAuthRepository(
    isSignedIn: Boolean
) : AuthRepository {

    @get:JvmName("getIsSignedInState")
    @set:JvmName("setIsSignedInState")
    var isSignedIn: Boolean = isSignedIn

    var signOutCount: Int = 0

    override fun googleSignInUrl(): String {
        return "https://example.test/oauth"
    }

    override suspend fun completeSignIn(accessToken: String, refreshToken: String): AppResult<Unit> {
        isSignedIn = true
        return AppResult.Success(Unit)
    }

    override fun isSignedIn(): Boolean {
        return isSignedIn
    }

    override suspend fun signOut(): AppResult<Unit> {
        signOutCount += 1
        isSignedIn = false
        return AppResult.Success(Unit)
    }

    override fun clearSession() {
        isSignedIn = false
    }
}
