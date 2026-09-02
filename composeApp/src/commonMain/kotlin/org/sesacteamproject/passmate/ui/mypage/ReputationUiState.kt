package org.sesacteamproject.passmate.ui.mypage

import org.sesacteamproject.passmate.user.domain.model.Badge
import org.sesacteamproject.passmate.user.domain.model.MyGrade
import org.sesacteamproject.passmate.user.domain.model.UserProfile

data class ReputationUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    // 시안 상단 프로필 카드(닉네임·캐릭터)용 — 등급 집계는 grade.stats가 담당한다
    val profile: UserProfile? = null,
    val grade: MyGrade? = null,
    val badges: List<Badge> = emptyList()
)
