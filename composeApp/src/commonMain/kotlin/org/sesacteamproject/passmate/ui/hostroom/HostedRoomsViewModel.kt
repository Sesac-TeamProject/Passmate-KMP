package org.sesacteamproject.passmate.ui.hostroom

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.auth.domain.usecase.IsSignedInUseCase
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.model.onFailure
import org.sesacteamproject.passmate.core.model.onSuccess
import org.sesacteamproject.passmate.mvi.MviViewModel
import org.sesacteamproject.passmate.room.domain.usecase.GetHostedRoomsUseCase
import org.sesacteamproject.passmate.user.domain.usecase.GetMyGradeUseCase

class HostedRoomsViewModel(
    private val getHostedRoomsUseCase: GetHostedRoomsUseCase,
    private val getMyGradeUseCase: GetMyGradeUseCase,
    private val isSignedInUseCase: IsSignedInUseCase
) : MviViewModel<HostedRoomsUiState, HostedRoomsAction, HostedRoomsEvent>(HostedRoomsUiState()) {

    private var hasEntered = false

    private fun onEnter() {
        if (hasEntered) {
            return
        }
        hasEntered = true
        // 회원 전용 가드 — 서버 검증이 최종 권위지만 UX상 진입 시 먼저 로그인 유도한다 (규칙 §8)
        if (!isSignedInUseCase.invoke()) {
            viewModelScope.launch {
                _event.emit(HostedRoomsEvent.RequireSignIn)
            }
        } else {
            load()
        }
    }

    private fun load() {
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            val gradeDeferred = async { getMyGradeUseCase.invoke() }
            val roomsDeferred = async { getHostedRoomsUseCase.invoke(null) }
            val gradeResult = gradeDeferred.await()
            val roomsResult = roomsDeferred.await()

            if (roomsResult is AppResult.Success) {
                val page = roomsResult.value

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadFailed = false,
                        // 명성 카드는 부가 정보 — 등급 로드 실패는 목록 표시를 막지 않는다
                        grade = (gradeResult as? AppResult.Success)?.value,
                        ongoing = page.items.filter { room -> room.isOngoing },
                        ended = page.items.filter { room -> !room.isOngoing },
                        nextCursor = if (page.hasNext) page.nextCursor else null
                    )
                }
            } else {
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
            getHostedRoomsUseCase.invoke(cursor)
                .onSuccess { page ->
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            ongoing = it.ongoing + page.items.filter { room -> room.isOngoing },
                            ended = it.ended + page.items.filter { room -> !room.isOngoing },
                            nextCursor = if (page.hasNext) page.nextCursor else null
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingMore = false) }
                    _event.emit(HostedRoomsEvent.ShowNotice("목록을 더 불러오지 못했어요"))
                }
        }
    }

    private fun onRoomCreated(pin: String) {
        load()
        emitNotice("방이 만들어졌어요 · PIN ${formatPin(pin)}")
    }

    private fun onClickCreate() {
        viewModelScope.launch {
            _event.emit(HostedRoomsEvent.OpenCreateSheet)
        }
    }

    private fun onClickReputation() {
        viewModelScope.launch {
            _event.emit(HostedRoomsEvent.OpenReputation)
        }
    }

    // 진행 리모컨(M-T2)은 후속 태스크에서 연결한다 (tasks.md T119)
    private fun onClickOngoingRoom() {
        emitNotice("진행 리모컨은 곧 제공돼요 · 프로젝터 화면은 웹에서 진행해 주세요")
    }

    private fun onClickEndedRoom(roomId: Long) {
        viewModelScope.launch {
            _event.emit(HostedRoomsEvent.OpenRoomReport(roomId))
        }
    }

    private fun emitNotice(message: String) {
        viewModelScope.launch {
            _event.emit(HostedRoomsEvent.ShowNotice(message))
        }
    }

    private fun formatPin(pin: String): String {
        return pin.chunked(3).joinToString(" ")
    }

    override fun onAction(action: HostedRoomsAction) {
        when (action) {
            is HostedRoomsAction.Enter -> onEnter()
            is HostedRoomsAction.Retry -> load()
            is HostedRoomsAction.LoadMore -> onLoadMore()
            is HostedRoomsAction.ClickCreate -> onClickCreate()
            is HostedRoomsAction.ClickReputation -> onClickReputation()
            is HostedRoomsAction.ClickOngoingRoom -> onClickOngoingRoom()
            is HostedRoomsAction.ClickEndedRoom -> onClickEndedRoom(action.roomId)
            is HostedRoomsAction.RoomCreated -> onRoomCreated(action.pin)
            is HostedRoomsAction.Notice -> emitNotice(action.message)
        }
    }
}
