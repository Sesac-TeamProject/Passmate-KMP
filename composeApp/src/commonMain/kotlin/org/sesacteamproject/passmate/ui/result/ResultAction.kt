package org.sesacteamproject.passmate.ui.result

sealed interface ResultAction {

    data class Enter(val roomId: Long) : ResultAction

    data class SelectQuestion(val questionNo: Int) : ResultAction

    data object ClickExport : ResultAction

    data object ClickSignup : ResultAction

    data object Retry : ResultAction
}
