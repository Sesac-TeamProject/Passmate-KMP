package org.sesacteamproject.passmate.ui.home

import org.sesacteamproject.passmate.payment.domain.model.PublicRoom
import org.sesacteamproject.passmate.payment.domain.model.RoomTypeFilter

// 공개 방 목록·탐색 화면 상태 (M-11)
data class RoomListUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val rooms: List<PublicRoom> = emptyList(),
    val query: String = "",
    val typeFilter: RoomTypeFilter = RoomTypeFilter.ALL,
    val hasNext: Boolean = false,
    val nextCursor: String? = null,
    val hasError: Boolean = false
) {
    val isEmpty: Boolean
        get() = !isLoading && !hasError && rooms.isEmpty()
}
