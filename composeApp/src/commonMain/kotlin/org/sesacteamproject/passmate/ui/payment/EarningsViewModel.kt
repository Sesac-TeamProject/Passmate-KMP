package org.sesacteamproject.passmate.ui.payment

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.auth.domain.usecase.IsSignedInUseCase
import org.sesacteamproject.passmate.core.model.onFailure
import org.sesacteamproject.passmate.core.model.onSuccess
import org.sesacteamproject.passmate.mvi.MviViewModel
import org.sesacteamproject.passmate.payment.domain.usecase.GetEarningsUseCase

class EarningsViewModel(
    private val getEarningsUseCase: GetEarningsUseCase,
    private val isSignedInUseCase: IsSignedInUseCase
) : MviViewModel<EarningsUiState, EarningsAction, EarningsEvent>(EarningsUiState()) {

    private var hasEntered = false

    private fun onEnter() {
        if (hasEntered) {
            return
        }
        hasEntered = true
        // 호스트(회원) 전용 가드 — 서버 검증이 최종 권위 (규칙 §8)
        if (!isSignedInUseCase.invoke()) {
            viewModelScope.launch {
                _event.emit(EarningsEvent.RequireSignIn)
            }
        } else {
            load()
        }
    }

    private fun load() {
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            getEarningsUseCase.invoke(null)
                .onSuccess { earnings ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadFailed = false,
                            earnings = earnings,
                            items = earnings.items,
                            nextCursor = if (earnings.hasNext) earnings.nextCursor else null
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false, loadFailed = true) }
                }
        }
    }

    private fun onLoadMore() {
        val state = _uiState.value
        val cursor = state.nextCursor

        if (cursor == null || state.isLoadingMore) {
            return
        }
        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            getEarningsUseCase.invoke(cursor)
                .onSuccess { earnings ->
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            items = it.items + earnings.items,
                            nextCursor = if (earnings.hasNext) earnings.nextCursor else null
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingMore = false) }
                    _event.emit(EarningsEvent.ShowNotice("내역을 더 불러오지 못했어요"))
                }
        }
    }

    private fun onClickViewAllHistory() {
        viewModelScope.launch {
            _event.emit(EarningsEvent.OpenCoinHistory)
        }
    }

    private fun onClickManageAccount() {
        viewModelScope.launch {
            _event.emit(EarningsEvent.OpenAccountSheet)
        }
    }

    private fun onAccountSaved() {
        load()
        viewModelScope.launch {
            _event.emit(EarningsEvent.ShowNotice("정산 계좌를 저장했어요"))
        }
    }

    private fun onNotice(message: String) {
        viewModelScope.launch {
            _event.emit(EarningsEvent.ShowNotice(message))
        }
    }

    override fun onAction(action: EarningsAction) {
        when (action) {
            is EarningsAction.Enter -> onEnter()
            is EarningsAction.Retry -> load()
            is EarningsAction.LoadMore -> onLoadMore()
            is EarningsAction.ClickViewAllHistory -> onClickViewAllHistory()
            is EarningsAction.ClickManageAccount -> onClickManageAccount()
            is EarningsAction.AccountSaved -> onAccountSaved()
            is EarningsAction.Notice -> onNotice(action.message)
        }
    }
}
