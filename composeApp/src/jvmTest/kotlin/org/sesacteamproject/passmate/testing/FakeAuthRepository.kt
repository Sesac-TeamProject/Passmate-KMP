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

    @get:JvmName("getIsDevSignInAvailableState")
    @set:JvmName("setIsDevSignInAvailableState")
    var isDevSignInAvailable: Boolean = false

    var devSignInResult: AppResult<Unit> = AppResult.Success(Unit)

    var devSignInCount: Int = 0

    var googleSignInResult: AppResult<Unit> = AppResult.Success(Unit)

    var googleIdToken: String? = null

    override suspend fun signInWithGoogle(idToken: String): AppResult<Unit> {
        googleIdToken = idToken

        return if (googleSignInResult is AppResult.Success) {
            completeSignIn()
        } else {
            googleSignInResult
        }
    }

    private fun completeSignIn(): AppResult<Unit> {
        isSignedIn = true
        return AppResult.Success(Unit)
    }

    override fun isDevSignInAvailable(): Boolean {
        return isDevSignInAvailable
    }

    override suspend fun devSignIn(): AppResult<Unit> {
        devSignInCount += 1

        return if (devSignInResult is AppResult.Success) {
            completeSignIn()
        } else {
            devSignInResult
        }
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
