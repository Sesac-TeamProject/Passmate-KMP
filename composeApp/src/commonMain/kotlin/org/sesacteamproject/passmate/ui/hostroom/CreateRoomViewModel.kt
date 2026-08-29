package org.sesacteamproject.passmate.ui.hostroom

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.onFailure
import org.sesacteamproject.passmate.core.model.onSuccess
import org.sesacteamproject.passmate.mvi.MviViewModel
import org.sesacteamproject.passmate.question.domain.usecase.GetMyQuestionSetsUseCase
import org.sesacteamproject.passmate.room.domain.usecase.CreateRoomUseCase

class CreateRoomViewModel(
    private val getMyQuestionSetsUseCase: GetMyQuestionSetsUseCase,
    private val createRoomUseCase: CreateRoomUseCase
) : MviViewModel<CreateRoomUiState, CreateRoomAction, CreateRoomEvent>(CreateRoomUiState()) {

    private var hasEntered = false

    private fun onEnter() {
        if (hasEntered) {
            return
        }
        hasEntered = true
        loadSets()
    }

    private fun loadSets() {
        _uiState.update { it.copy(isLoadingSets = true, setsLoadFailed = false) }
        viewModelScope.launch {
            // 방에는 확정(CONFIRMED) 세트만 연결 가능 — 검토 단계 강제 (FR-010)
            getMyQuestionSetsUseCase.invoke(confirmedOnly = true, cursor = null)
                .onSuccess { page ->
                    _uiState.update {
                        it.copy(
                            isLoadingSets = false,
                            setsLoadFailed = false,
                            sets = page.items,
                            selectedSetId = it.selectedSetId ?: page.items.firstOrNull()?.setId
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingSets = false, setsLoadFailed = true) }
                }
        }
    }

    private fun onSubmit() {
        val state = _uiState.value

        if (!state.canSubmit) {
            return
        }
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            createRoomUseCase.invoke(
                title = state.title,
                questionSetId = state.selectedSetId,
                isPaid = state.isPaid,
                entryFee = state.entryFeeText.toIntOrNull()
            )
                .onSuccess { created ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    _event.emit(CreateRoomEvent.Created(created.pin))
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    _event.emit(CreateRoomEvent.ShowNotice(createFailMessage(error)))
                }
        }
    }

    // 서버 code 기반 문구 분기 (규칙 §10) — 최종 권위는 서버 검증
    private fun createFailMessage(error: AppError): String {
        return if (error.serverCode == "HOST_LEVEL_REQUIRED") {
            "유료 방은 Lv.3(검증된 운영자)부터 열 수 있어요"
        } else if (error is AppError.ValidationFailed) {
            error.serverMessage ?: "입력값을 확인해 주세요"
        } else if (error is AppError.NetworkError) {
            "네트워크 연결을 확인해 주세요"
        } else {
            "방을 만들지 못했어요. 다시 시도해 주세요"
        }
    }

    override fun onAction(action: CreateRoomAction) {
        when (action) {
            is CreateRoomAction.Enter -> onEnter()
            is CreateRoomAction.RetrySets -> loadSets()
            is CreateRoomAction.ChangeTitle -> _uiState.update { it.copy(title = action.title) }
            is CreateRoomAction.SelectSet -> _uiState.update { it.copy(selectedSetId = action.setId) }
            is CreateRoomAction.SelectPaid -> _uiState.update { it.copy(isPaid = action.isPaid) }
            is CreateRoomAction.ChangeEntryFee -> _uiState.update {
                it.copy(entryFeeText = action.text.filter { ch -> ch.isDigit() }.take(7))
            }
            is CreateRoomAction.Submit -> onSubmit()
        }
    }
}
