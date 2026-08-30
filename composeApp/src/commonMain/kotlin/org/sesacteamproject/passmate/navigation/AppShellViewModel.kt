package org.sesacteamproject.passmate.navigation

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.auth.domain.usecase.IsSignedInUseCase
import org.sesacteamproject.passmate.mvi.MviViewModel

// 하단 탭 게스트 가드 — 로그인 필수 탭은 화면을 열지 않고 SignIn으로 돌린다 (규칙 §8, 결정 2).
// 탭을 누를 때마다 동기 조회하므로 로그인/로그아웃 후 별도 갱신이 필요 없다.
class AppShellViewModel(
    private val isSignedInUseCase: IsSignedInUseCase
) : MviViewModel<AppShellUiState, AppShellAction, AppShellEvent>(AppShellUiState()) {

    private fun onSelectTab(tab: AppTab) {
        val isSignedIn = isSignedInUseCase.invoke()

        _uiState.update { it.copy(isSignedIn = isSignedIn) }
        viewModelScope.launch {
            if (tab.requiresSignIn && !isSignedIn) {
                _event.emit(AppShellEvent.RequireSignIn)
            } else {
                _event.emit(AppShellEvent.NavigateToTab(tab))
            }
        }
    }

    override fun onAction(action: AppShellAction) {
        when (action) {
            is AppShellAction.SelectTab -> onSelectTab(action.tab)
        }
    }
}
