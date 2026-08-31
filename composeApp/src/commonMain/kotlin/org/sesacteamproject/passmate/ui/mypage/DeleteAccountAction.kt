package org.sesacteamproject.passmate.ui.mypage

sealed interface DeleteAccountAction {

    data object Enter : DeleteAccountAction

    // "위 내용을 확인했어요" 체크 토글 — 체크해야만 탈퇴할 수 있다
    data object ToggleConfirm : DeleteAccountAction

    data object ClickDelete : DeleteAccountAction
}
