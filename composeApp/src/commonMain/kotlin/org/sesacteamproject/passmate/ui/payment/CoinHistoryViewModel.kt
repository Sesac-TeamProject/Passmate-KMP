package org.sesacteamproject.passmate.ui.payment

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.core.model.onFailure
import org.sesacteamproject.passmate.core.model.onSuccess
import org.sesacteamproject.passmate.mvi.MviViewModel
import org.sesacteamproject.passmate.payment.domain.usecase.GetCoinTransactionsUseCase

class CoinHistoryViewModel(
    private val getCoinTransactionsUseCase: GetCoinTransactionsUseCase
) : MviViewModel<CoinHistoryUiState, CoinHistoryAction, CoinHistoryEvent>(CoinHistoryUiState()) {

    private var isEntered = false

    private fun onEnter() {
        if (isEntered) {
            return
        }
        isEntered = true
        load()
    }

    private fun load() {
        _uiState.update { it.copy(isLoading = true, hasError = false) }
        viewModelScope.launch {
            getCoinTransactionsUseCase.invoke(cursor = null)
                .onSuccess { page ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasError = false,
                            items = page.items,
                            hasNext = page.hasNext,
                            nextCursor = page.nextCursor
                        )
                    }
                }
                .onFailure { _uiState.update { it.copy(isLoading = false, hasError = true) } }
        }
    }

    private fun onLoadMore() {
        val state = _uiState.value

        if (state.isLoading || state.isLoadingMore || !state.hasNext) {
            return
        }
        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            getCoinTransactionsUseCase.invoke(cursor = state.nextCursor)
                .onSuccess { page ->
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            items = it.items + page.items,
                            hasNext = page.hasNext,
                            nextCursor = page.nextCursor
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingMore = false) }
                    _event.emit(CoinHistoryEvent.ShowNotice("내역을 더 불러오지 못했어요"))
                }
        }
    }

    override fun onAction(action: CoinHistoryAction) {
        when (action) {
            is CoinHistoryAction.Enter -> onEnter()
            is CoinHistoryAction.Retry -> load()
            is CoinHistoryAction.LoadMore -> onLoadMore()
        }
    }
}
