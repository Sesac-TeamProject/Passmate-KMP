package org.sesacteamproject.passmate.ui.auth

sealed interface SignInEvent {

    data class OpenSignInPage(val url: String) : SignInEvent

    data object SignInCompleted : SignInEvent

    data object GuestEnterRequested : SignInEvent

    data class ShowNotice(val message: String) : SignInEvent
}
