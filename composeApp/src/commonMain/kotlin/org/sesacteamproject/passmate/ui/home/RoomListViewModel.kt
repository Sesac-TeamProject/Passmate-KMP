package org.sesacteamproject.passmate.ui.home

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.core.model.onFailure
import org.sesacteamproject.passmate.core.model.onSuccess
import org.sesacteamproject.passmate.mvi.MviViewModel
import org.sesacteamproject.passmate.payment.domain.model.RoomSort
import org.sesacteamproject.passmate.payment.domain.model.RoomTypeFilter
import org.sesacteamproject.passmate.payment.domain.usecase.GetPublicRoomsUseCase
import org.sesacteamproject.passmate.room.domain.usecase.GetRoomPinUseCase

class RoomListViewModel(
    private val getPublicRoomsUseCase: GetPublicRoomsUseCase,
    private val getRoomPinUseCase: GetRoomPinUseCase
) : MviViewModel<RoomListUiState, RoomListAction, RoomListEvent>(RoomListUiState()) {

    private var loadJob: Job? = null

    private fun onChangeQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    private fun onSubmitSearch() {
        reload()
    }

    private fun onSelectType(type: RoomTypeFilter) {
        if (_uiState.value.typeFilter != type) {
            _uiState.update { it.copy(typeFilter = type) }
            reload()
        }
    }

    private fun reload() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, hasError = false) }
        loadJob = viewModelScope.launch {
            val state = _uiState.value

            getPublicRoomsUseCase.invoke(
                sort = RoomSort.POPULAR,
                query = state.query.trim().ifEmpty { null },
                type = state.typeFilter,
                cursor = null
            )
                .onSuccess { page ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasError = false,
                            rooms = page.items,
                            hasNext = page.hasNext,
                            nextCursor = page.nextCursor
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false, hasError = true) }
                }
        }
    }

    private fun onLoadMore() {
        val state = _uiState.value

        if (state.isLoading || state.isLoadingMore || !state.hasNext) {
            return
        }
        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            getPublicRoomsUseCase.invoke(
                sort = RoomSort.POPULAR,
                query = state.query.trim().ifEmpty { null },
                type = state.typeFilter,
                cursor = state.nextCursor
            )
                .onSuccess { page ->
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            rooms = it.rooms + page.items,
                            hasNext = page.hasNext,
                            nextCursor = page.nextCursor
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingMore = false) }
                    _event.emit(RoomListEvent.ShowNotice("목록을 더 불러오지 못했어요"))
                }
        }
    }

    // 목록 응답에 pin이 없어 roomId로 한 번 더 조회한다 (계약 `PublicRoomResponse`)
    private fun onClickRoom(roomId: Long) {
        viewModelScope.launch {
            getRoomPinUseCase.invoke(roomId)
                .onSuccess { pin -> _event.emit(RoomListEvent.OpenRoom(pin)) }
                .onFailure { _event.emit(RoomListEvent.ShowNotice("방 정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요")) }
        }
    }

    private fun onClickPinEntry() {
        viewModelScope.launch {
            _event.emit(RoomListEvent.OpenPinEntry)
        }
    }

    private fun onClickHost(hostId: Long) {
        viewModelScope.launch {
            _event.emit(RoomListEvent.OpenHostProfile(hostId))
        }
    }

    private fun onNotice(message: String) {
        viewModelScope.launch {
            _event.emit(RoomListEvent.ShowNotice(message))
        }
    }

    override fun onAction(action: RoomListAction) {
        when (action) {
            is RoomListAction.ChangeQuery -> onChangeQuery(action.query)
            is RoomListAction.SubmitSearch -> onSubmitSearch()
            is RoomListAction.SelectType -> onSelectType(action.type)
            is RoomListAction.ClickRoom -> onClickRoom(action.roomId)
            is RoomListAction.ClickHost -> onClickHost(action.hostId)
            is RoomListAction.LoadMore -> onLoadMore()
            is RoomListAction.Retry -> reload()
            is RoomListAction.ClickPinEntry -> onClickPinEntry()
            is RoomListAction.Notice -> onNotice(action.message)
        }
    }

    init {
        reload()
    }
}
