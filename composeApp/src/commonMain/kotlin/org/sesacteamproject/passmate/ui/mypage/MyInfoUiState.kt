package org.sesacteamproject.passmate.ui.mypage

import org.sesacteamproject.passmate.user.domain.model.JoinedRoom
import org.sesacteamproject.passmate.user.domain.model.MyPageSummary
import org.sesacteamproject.passmate.user.domain.model.OngoingRoom

data class MyInfoUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val summary: MyPageSummary? = null,
    val ongoing: OngoingRoom? = null,
    val rooms: List<JoinedRoom> = emptyList(),
    val nextCursor: String? = null,
    val isLoadingMore: Boolean = false
)
