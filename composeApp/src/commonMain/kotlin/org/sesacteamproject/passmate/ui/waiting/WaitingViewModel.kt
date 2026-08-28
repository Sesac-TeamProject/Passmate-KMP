package org.sesacteamproject.passmate.ui.waiting

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.onFailure
import org.sesacteamproject.passmate.core.model.onSuccess
import org.sesacteamproject.passmate.core.network.SessionEventStream
import org.sesacteamproject.passmate.core.network.event.ServerEvent
import org.sesacteamproject.passmate.mvi.MviViewModel
import org.sesacteamproject.passmate.room.domain.model.Participant
import org.sesacteamproject.passmate.room.domain.model.RoomStatus
import org.sesacteamproject.passmate.room.domain.usecase.GetMyParticipationUseCase
import org.sesacteamproject.passmate.room.domain.usecase.GetParticipantsUseCase
import org.sesacteamproject.passmate.room.domain.usecase.GetRoomInfoUseCase
import org.sesacteamproject.passmate.room.domain.usecase.LeaveRoomUseCase

class WaitingViewModel(
    private val getRoomInfoUseCase: GetRoomInfoUseCase,
    private val getParticipantsUseCase: GetParticipantsUseCase,
    private val leaveRoomUseCase: LeaveRoomUseCase,
    private val getMyParticipationUseCase: GetMyParticipationUseCase,
    private val sessionEventStream: SessionEventStream
) : MviViewModel<WaitingUiState, WaitingAction, WaitingEvent>(WaitingUiState()) {

    private var roomId: Long? = null

    private var eventsJob: Job? = null

    private fun onEnter(pin: String) {
        if (roomId != null) {
            return
        }
        val my = getMyParticipationUseCase.invoke()

        _uiState.update {
            it.copy(
                pin = pin,
                myParticipantId = my?.participantId,
                myNickname = my?.nickname
            )
        }
        viewModelScope.launch {
            getRoomInfoUseCase.invoke(pin)
                .onSuccess { room ->
                    roomId = room.roomId
                    _uiState.update { it.copy(isLoading = false, roomTitle = room.title) }
                    // 늦은 입장(FR-024) — 이미 진행 중이면 바로 풀이 화면으로 전환한다
                    if (room.status == RoomStatus.RUNNING) {
                        _event.emit(WaitingEvent.SessionStarted(pin))
                    } else {
                        observeRoomEvents(room.roomId)
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _event.emit(WaitingEvent.RoomClosed(roomErrorMessage(error)))
                }
        }
    }

    // 초기 목록·재접속 복구는 REST 조회, 이후 증분은 WS 이벤트 (규칙 §2-1-2)
    private fun observeRoomEvents(roomId: Long) {
        eventsJob?.cancel()
        eventsJob = viewModelScope.launch {
            sessionEventStream.events(roomId).collect { streamEvent ->
                when (streamEvent) {
                    is SessionEventStream.StreamEvent.Connected -> refreshParticipants(roomId)
                    is SessionEventStream.StreamEvent.Received -> handleServerEvent(streamEvent.frame.event)
                    is SessionEventStream.StreamEvent.Disconnected -> Unit
                }
            }
        }
    }

    private suspend fun refreshParticipants(roomId: Long) {
        getParticipantsUseCase.invoke(roomId).onSuccess { participants ->
            _uiState.update {
                it.copy(
                    isLoading = false,
                    participants = participants,
                    totalCount = participants.size
                )
            }
        }
    }

    private suspend fun handleServerEvent(event: ServerEvent) {
        when (event) {
            is ServerEvent.ParticipantJoined -> onParticipantJoined(event)
            is ServerEvent.ParticipantLeft -> onParticipantLeft(event)
            is ServerEvent.SessionStarted -> _event.emit(WaitingEvent.SessionStarted(_uiState.value.pin))
            is ServerEvent.RoomCancelled -> _event.emit(WaitingEvent.RoomClosed("방이 취소됐어요"))
            else -> Unit
        }
    }

    private fun onParticipantJoined(event: ServerEvent.ParticipantJoined) {
        val joined = Participant(
            participantId = event.participantId,
            nickname = event.nickname,
            avatarId = event.avatarId,
            isGuest = event.isGuest,
            isConnected = true
        )

        _uiState.update { state ->
            val others = state.participants.filter { it.participantId != joined.participantId }

            state.copy(
                participants = others + joined,
                totalCount = event.count
            )
        }
    }

    private suspend fun onParticipantLeft(event: ServerEvent.ParticipantLeft) {
        val isMe = event.participantId == _uiState.value.myParticipantId

        if (isMe && event.reason == ServerEvent.ParticipantLeft.REASON_KICKED) {
            _event.emit(WaitingEvent.RoomClosed("선생님이 내보냈어요"))
        } else {
            _uiState.update { state ->
                state.copy(
                    participants = state.participants.filter { it.participantId != event.participantId },
                    totalCount = event.count
                )
            }
        }
    }

    private fun onClickLeave() {
        val leavingRoomId = roomId

        eventsJob?.cancel()
        viewModelScope.launch {
            if (leavingRoomId != null) {
                leaveRoomUseCase.invoke(leavingRoomId)
            }
            _event.emit(WaitingEvent.Left)
        }
    }

    private fun roomErrorMessage(error: AppError): String {
        return when (error) {
            is AppError.NotFound -> "방을 찾을 수 없어요"
            is AppError.Gone -> "이미 종료된 방이에요"
            is AppError.NetworkError -> "네트워크 연결을 확인해 주세요"
            else -> "대기실 정보를 불러오지 못했어요"
        }
    }

    override fun onAction(action: WaitingAction) {
        when (action) {
            is WaitingAction.Enter -> onEnter(action.pin)
            is WaitingAction.ClickLeave -> onClickLeave()
        }
    }
}
