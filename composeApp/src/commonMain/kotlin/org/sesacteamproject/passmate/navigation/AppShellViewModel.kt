package org.sesacteamproject.passmate.navigation

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.auth.domain.usecase.IsSignedInUseCase
import org.sesacteamproject.passmate.mvi.MviViewModel

// 하단 탭 게스트 가드 + pendingRoute 보관 (규칙 §7·§8, 스펙 §2-2).
// 탭을 누를 때마다 로그인 여부를 동기 조회하므로 로그인/로그아웃 후 별도 갱신이 필요 없다.
class AppShellViewModel(
    private val isSignedInUseCase: IsSignedInUseCase
) : MviViewModel<AppShellUiState, AppShellAction, AppShellEvent>(AppShellUiState()) {

    private fun onSelectTab(tab: AppTab) {
        val isSignedIn = isSignedInUseCase.invoke()
        val isGuarded = tab.requiresSignIn && !isSignedIn

        _uiState.update {
            if (isGuarded) {
                it.copy(isSignedIn = isSignedIn, pendingRoute = NavigationAction.NavigateToTab(tab))
            } else {
                it.copy(isSignedIn = isSignedIn)
            }
        }
        viewModelScope.launch {
            if (isGuarded) {
                _event.emit(AppShellEvent.RequireSignIn)
            } else {
                _event.emit(AppShellEvent.NavigateToTab(tab))
            }
        }
    }

    private fun onRememberPendingRoute(pendingRoute: NavigationAction?) {
        _uiState.update { it.copy(pendingRoute = pendingRoute) }
    }

    private fun onResumeAfterSignIn() {
        val pendingRoute = _uiState.value.pendingRoute
        val isSignedIn = isSignedInUseCase.invoke()

        _uiState.update { it.copy(isSignedIn = isSignedIn, pendingRoute = null) }
        viewModelScope.launch {
            if (pendingRoute != null) {
                _event.emit(AppShellEvent.ResumePendingRoute(pendingRoute))
            } else {
                _event.emit(AppShellEvent.NavigateToHome)
            }
        }
    }

    override fun onAction(action: AppShellAction) {
        when (action) {
            is AppShellAction.SelectTab -> onSelectTab(action.tab)
            is AppShellAction.RememberPendingRoute -> onRememberPendingRoute(action.pendingRoute)
            is AppShellAction.ResumeAfterSignIn -> onResumeAfterSignIn()
        }
    }
}
