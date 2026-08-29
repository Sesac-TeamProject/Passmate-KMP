package org.sesacteamproject.passmate.ui.profile

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.auth.domain.usecase.IsSignedInUseCase
import org.sesacteamproject.passmate.core.model.onFailure
import org.sesacteamproject.passmate.core.model.onSuccess
import org.sesacteamproject.passmate.mvi.MviViewModel
import org.sesacteamproject.passmate.user.domain.model.ReportReason
import org.sesacteamproject.passmate.user.domain.usecase.BlockHostUseCase
import org.sesacteamproject.passmate.user.domain.usecase.GetHostProfileUseCase
import org.sesacteamproject.passmate.user.domain.usecase.ReportHostUseCase

class HostProfileViewModel(
    private val getHostProfileUseCase: GetHostProfileUseCase,
    private val blockHostUseCase: BlockHostUseCase,
    private val reportHostUseCase: ReportHostUseCase,
    private val isSignedInUseCase: IsSignedInUseCase
) : MviViewModel<HostProfileUiState, HostProfileAction, HostProfileEvent>(HostProfileUiState()) {

    private var loadedHostId: Long? = null

    private fun onEnter(hostId: Long) {
        if (loadedHostId != hostId) {
            load(hostId)
        }
    }

    private fun load(hostId: Long) {
        loadedHostId = hostId
        _uiState.update { HostProfileUiState(isLoading = true) }
        viewModelScope.launch {
            getHostProfileUseCase.invoke(hostId)
                .onSuccess { profile ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadFailed = false,
                            profile = profile
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false, loadFailed = true) }
                }
        }
    }

    private fun onClickRoom(pin: String) {
        viewModelScope.launch {
            _event.emit(HostProfileEvent.JoinRoom(pin))
        }
    }

    private fun onClickBlock() {
        val profile = _uiState.value.profile

        if (profile == null || _uiState.value.isSubmitting) {
            return
        }
        // 차단은 회원 전용 — 게스트는 로그인 유도 (규칙 §8)
        if (!isSignedInUseCase.invoke()) {
            viewModelScope.launch {
                _event.emit(HostProfileEvent.RequireSignIn)
            }
        } else {
            _uiState.update { it.copy(isSubmitting = true) }
            viewModelScope.launch {
                blockHostUseCase.invoke(profile.userId)
                    .onSuccess {
                        _uiState.update { it.copy(isSubmitting = false) }
                        _event.emit(HostProfileEvent.BlockedAndClose)
                    }
                    .onFailure {
                        _uiState.update { it.copy(isSubmitting = false) }
                        _event.emit(HostProfileEvent.ShowNotice("차단하지 못했어요. 다시 시도해 주세요"))
                    }
            }
        }
    }

    private fun onSubmitReport(reason: ReportReason) {
        val profile = _uiState.value.profile

        if (profile == null || _uiState.value.isSubmitting) {
            return
        }
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            reportHostUseCase.invoke(profile.userId, reason, null)
                .onSuccess {
                    _uiState.update { it.copy(isSubmitting = false, isReported = true) }
                }
                .onFailure {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _event.emit(HostProfileEvent.ShowNotice("신고를 접수하지 못했어요. 다시 시도해 주세요"))
                }
        }
    }

    override fun onAction(action: HostProfileAction) {
        when (action) {
            is HostProfileAction.Enter -> onEnter(action.hostId)
            is HostProfileAction.Retry -> load(action.hostId)
            is HostProfileAction.ClickRoom -> onClickRoom(action.pin)
            is HostProfileAction.ClickBlock -> onClickBlock()
            is HostProfileAction.SubmitReport -> onSubmitReport(action.reason)
        }
    }
}
