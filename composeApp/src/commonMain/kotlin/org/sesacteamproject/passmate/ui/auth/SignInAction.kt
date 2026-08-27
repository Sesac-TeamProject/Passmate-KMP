package org.sesacteamproject.passmate.ui.auth

sealed interface SignInAction {

    data object ClickGoogleSignIn : SignInAction

    data object ClickAppleSignIn : SignInAction

    data object ClickGuestEnter : SignInAction

    data class ReceiveOAuthCallback(
        val accessToken: String,
        val refreshToken: String
    ) : SignInAction
}
