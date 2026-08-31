package org.sesacteamproject.passmate.ui.mypage

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.auth.domain.usecase.IsSignedInUseCase
import org.sesacteamproject.passmate.mvi.MviViewModel

// 설정 (마이 탭 우상단 "설정") — 회원 탈퇴 진입점만 둔다. 탈퇴 자체는 DeleteAccount 화면(M-12-12)이 맡는다
class SettingsViewModel(
    private val isSignedInUseCase: IsSignedInUseCase
) : MviViewModel<SettingsUiState, SettingsAction, SettingsEvent>(SettingsUiState()) {

    private fun onEnter() {
        // 회원 전용 가드 — 서버 검증이 최종 권위 (규칙 §8)
        if (!isSignedInUseCase.invoke()) {
            viewModelScope.launch {
                _event.emit(SettingsEvent.RequireSignIn)
            }
        }
    }

    override fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.Enter -> onEnter()
        }
    }
}
