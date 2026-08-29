package org.sesacteamproject.passmate.ui.profile

import org.sesacteamproject.passmate.user.domain.model.ReportReason

sealed interface HostProfileAction {

    data class Enter(val hostId: Long) : HostProfileAction

    data class Retry(val hostId: Long) : HostProfileAction

    data class ClickRoom(val pin: String) : HostProfileAction

    data object ClickBlock : HostProfileAction

    data class SubmitReport(val reason: ReportReason) : HostProfileAction
}
