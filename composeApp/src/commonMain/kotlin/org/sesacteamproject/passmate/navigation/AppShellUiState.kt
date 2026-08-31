package org.sesacteamproject.passmate.navigation

// 셸 상태 — 마지막 탭 선택 시점의 로그인 여부 (탭 탭마다 동기 재조회, 규칙 §8)
data class AppShellUiState(
    val isSignedIn: Boolean = false
)
