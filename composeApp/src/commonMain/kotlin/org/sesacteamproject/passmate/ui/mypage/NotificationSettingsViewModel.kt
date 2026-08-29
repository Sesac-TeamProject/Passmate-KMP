package org.sesacteamproject.passmate.ui.mypage

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.core.model.onFailure
import org.sesacteamproject.passmate.core.model.onSuccess
import org.sesacteamproject.passmate.mvi.MviViewModel
import org.sesacteamproject.passmate.user.domain.model.NotificationSettings
import org.sesacteamproject.passmate.user.domain.usecase.GetNotificationSettingsUseCase
import org.sesacteamproject.passmate.user.domain.usecase.UpdateNotificationSettingsUseCase

class NotificationSettingsViewModel(
    private val getNotificationSettingsUseCase: GetNotificationSettingsUseCase,
    private val updateNotificationSettingsUseCase: UpdateNotificationSettingsUseCase
) : MviViewModel<NotificationSettingsUiState, NotificationSettingsAction, NotificationSettingsEvent>(
    NotificationSettingsUiState()
) {

    private var hasEntered = false

    private fun onEnter() {
        if (hasEntered) {
            return
        }
        hasEntered = true
        load()
    }

    private fun load() {
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            getNotificationSettingsUseCase.invoke()
                .onSuccess { settings ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadFailed = false,
                            sessionStart = settings.sessionStart,
                            ratingRequest = settings.ratingRequest,
                            settlementDone = settings.settlementDone
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false, loadFailed = true) }
                }
        }
    }

    // 토글 즉시 저장 — 낙관 반영 후 실패 시 원복한다 (M-12-10)
    private fun onToggle(kind: NotificationKind) {
        val before = _uiState.value

        if (before.isSaving || before.isLoading) {
            return
        }
        val after = when (kind) {
            NotificationKind.SESSION_START -> before.copy(sessionStart = !before.sessionStart)
            NotificationKind.RATING_REQUEST -> before.copy(ratingRequest = !before.ratingRequest)
            NotificationKind.SETTLEMENT_DONE -> before.copy(settlementDone = !before.settlementDone)
        }

        _uiState.update { after.copy(isSaving = true) }
        viewModelScope.launch {
            val settings = NotificationSettings(
                sessionStart = after.sessionStart,
                ratingRequest = after.ratingRequest,
                settlementDone = after.settlementDone
            )

            updateNotificationSettingsUseCase.invoke(settings)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false) }
                }
                .onFailure {
                    _uiState.update { before.copy(isSaving = false) }
                    _event.emit(NotificationSettingsEvent.ShowNotice("설정을 저장하지 못했어요"))
                }
        }
    }

    override fun onAction(action: NotificationSettingsAction) {
        when (action) {
            is NotificationSettingsAction.Enter -> onEnter()
            is NotificationSettingsAction.Retry -> load()
            is NotificationSettingsAction.Toggle -> onToggle(action.kind)
        }
    }
}
