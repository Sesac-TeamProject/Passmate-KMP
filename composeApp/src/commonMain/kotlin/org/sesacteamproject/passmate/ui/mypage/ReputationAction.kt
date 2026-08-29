package org.sesacteamproject.passmate.ui.mypage

sealed interface ReputationAction {

    data object Enter : ReputationAction

    data object Retry : ReputationAction
}
