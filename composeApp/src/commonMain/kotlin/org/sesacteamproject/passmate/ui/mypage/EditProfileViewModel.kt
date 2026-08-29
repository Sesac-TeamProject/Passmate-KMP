package org.sesacteamproject.passmate.ui.mypage

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.onFailure
import org.sesacteamproject.passmate.core.model.onSuccess
import org.sesacteamproject.passmate.mvi.MviViewModel
import org.sesacteamproject.passmate.user.domain.usecase.UpdateMyProfileUseCase

class EditProfileViewModel(
    private val updateMyProfileUseCase: UpdateMyProfileUseCase
) : MviViewModel<EditProfileUiState, EditProfileAction, EditProfileEvent>(EditProfileUiState()) {

    private var hasEntered = false

    private fun onEnter(nickname: String, avatarId: Int?) {
        if (hasEntered) {
            return
        }
        hasEntered = true
        _uiState.update { it.copy(nickname = nickname, avatarId = avatarId) }
    }

    private fun onSubmit() {
        val state = _uiState.value

        if (!state.canSubmit) {
            return
        }
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            updateMyProfileUseCase.invoke(state.nickname, state.avatarId)
                .onSuccess {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _event.emit(EditProfileEvent.Saved)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    _event.emit(EditProfileEvent.ShowNotice(saveFailMessage(error)))
                }
        }
    }

    // 서버 code 기반 문구 분기 (규칙 §10) — 닉네임 최종 검증은 서버가 한다
    private fun saveFailMessage(error: AppError): String {
        return if (error is AppError.ValidationFailed) {
            error.serverMessage ?: "닉네임을 확인해 주세요"
        } else if (error is AppError.NetworkError) {
            "네트워크 연결을 확인해 주세요"
        } else {
            "저장하지 못했어요. 다시 시도해 주세요"
        }
    }

    override fun onAction(action: EditProfileAction) {
        when (action) {
            is EditProfileAction.Enter -> onEnter(action.nickname, action.avatarId)
            is EditProfileAction.ChangeNickname -> _uiState.update {
                it.copy(nickname = action.text.take(NICKNAME_MAX_LENGTH))
            }
            is EditProfileAction.SelectAvatar -> _uiState.update { it.copy(avatarId = action.avatarId) }
            is EditProfileAction.Submit -> onSubmit()
        }
    }

    companion object {
        private const val NICKNAME_MAX_LENGTH = 12
    }
}
