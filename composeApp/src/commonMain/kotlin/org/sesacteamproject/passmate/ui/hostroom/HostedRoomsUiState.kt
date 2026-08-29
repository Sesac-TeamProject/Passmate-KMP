package org.sesacteamproject.passmate.ui.hostroom

import org.sesacteamproject.passmate.room.domain.model.HostedRoom
import org.sesacteamproject.passmate.user.domain.model.MyGrade

data class HostedRoomsUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val grade: MyGrade? = null,
    val ongoing: List<HostedRoom> = emptyList(),
    val ended: List<HostedRoom> = emptyList(),
    val nextCursor: String? = null,
    val isLoadingMore: Boolean = false
)
