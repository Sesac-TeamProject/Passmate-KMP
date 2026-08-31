package org.sesacteamproject.passmate.ui.mypage

sealed interface DeleteAccountEvent {

    // 탈퇴 완료 → 홈으로 (세션 정리는 shared가 수행)
    data object Deleted : DeleteAccountEvent

    data class ShowNotice(val message: String) : DeleteAccountEvent
}
