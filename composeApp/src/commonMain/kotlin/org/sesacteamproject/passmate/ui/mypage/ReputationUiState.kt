package org.sesacteamproject.passmate.ui.mypage

import org.sesacteamproject.passmate.user.domain.model.Badge
import org.sesacteamproject.passmate.user.domain.model.MyGrade

data class ReputationUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val grade: MyGrade? = null,
    val badges: List<Badge> = emptyList()
)
