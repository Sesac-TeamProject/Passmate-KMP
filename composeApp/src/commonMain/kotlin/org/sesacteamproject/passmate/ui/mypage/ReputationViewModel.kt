package org.sesacteamproject.passmate.ui.mypage

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.auth.domain.usecase.IsSignedInUseCase
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.mvi.MviViewModel
import org.sesacteamproject.passmate.user.domain.usecase.GetMyBadgesUseCase
import org.sesacteamproject.passmate.user.domain.usecase.GetMyGradeUseCase

class ReputationViewModel(
    private val getMyGradeUseCase: GetMyGradeUseCase,
    private val getMyBadgesUseCase: GetMyBadgesUseCase,
    private val isSignedInUseCase: IsSignedInUseCase
) : MviViewModel<ReputationUiState, ReputationAction, ReputationEvent>(ReputationUiState()) {

    private var hasEntered = false

    private fun onEnter() {
        if (hasEntered) {
            return
        }
        hasEntered = true
        // 회원 전용 가드 — 서버 검증이 최종 권위지만 UX상 진입 시 먼저 로그인 유도한다 (규칙 §8)
        if (!isSignedInUseCase.invoke()) {
            viewModelScope.launch {
                _event.emit(ReputationEvent.RequireSignIn)
            }
        } else {
            load()
        }
    }

    private fun load() {
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            val gradeDeferred = async { getMyGradeUseCase.invoke() }
            val badgesDeferred = async { getMyBadgesUseCase.invoke() }
            val gradeResult = gradeDeferred.await()
            val badgesResult = badgesDeferred.await()

            if (gradeResult is AppResult.Success && badgesResult is AppResult.Success) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadFailed = false,
                        grade = gradeResult.value,
                        badges = badgesResult.value
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    override fun onAction(action: ReputationAction) {
        when (action) {
            is ReputationAction.Enter -> onEnter()
            is ReputationAction.Retry -> load()
        }
    }
}
