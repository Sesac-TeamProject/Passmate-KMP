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
import org.sesacteamproject.passmate.room.domain.model.RoomInfo
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

    // 풀이 화면으로 넘긴 뒤인가 — 넘긴 뒤라면 세션 종료로 사용자를 끌어내지 않는다
    private var hasHandedOffToPlay: Boolean = false

    private fun onEnter(pin: String) {
        val isReturning = roomId != null

        if (isReturning) {
            recheckStatus(pin)
        } else {
            loadRoom(pin)
        }
    }

    private fun loadRoom(pin: String) {
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
                    _uiState.update { it.copy(isLoading = false, roomTitle = room.title) }
                    routeByStatus(room, pin)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _event.emit(WaitingEvent.RoomClosed(roomErrorMessage(error)))
                }
        }
    }

    // 풀이 화면에서 뒤로가기로 돌아온 경로다. 그 사이 끝난 세션이면 "입장 완료" 화면에 머무르면 안 된다.
    // 이미 받아 둔 종료 신호가 있으면 즉시, 없으면 서버 상태를 다시 확인한다 (규칙 §2-1-2 재접속 복구).
    // 진행 중(RUNNING)이면 다시 풀이로 밀어 넣지 않는다 — 뒤로가기가 영영 먹히지 않게 된다
    private fun recheckStatus(pin: String) {
        val knownRoomId = roomId

        hasHandedOffToPlay = false
        viewModelScope.launch {
            if (_uiState.value.isSessionFinished && knownRoomId != null) {
                emitSessionFinished(knownRoomId)
            } else {
                getRoomInfoUseCase.invoke(pin)
                    .onSuccess { room ->
                        if (room.status == RoomStatus.FINISHED) {
                            emitSessionFinished(room.roomId)
                        } else {
                            enterWaitingRoom(room.roomId)
                        }
                    }
                    .onFailure { error -> _event.emit(WaitingEvent.RoomClosed(roomErrorMessage(error))) }
            }
        }
    }

    // 첫 진입의 라우트는 서버 상태가 정한다 (규칙 §2-1-2) —
    // WAITING은 대기실 유지, RUNNING은 늦은 입장(FR-024), FINISHED는 결과 화면
    private suspend fun routeByStatus(room: RoomInfo, pin: String) {
        roomId = room.roomId

        when (room.status) {
            RoomStatus.RUNNING -> emitSessionStarted(pin)
            RoomStatus.FINISHED -> emitSessionFinished(room.roomId)
            else -> enterWaitingRoom(room.roomId)
        }
    }

    // 구독을 먼저 걸어 증분을 놓치지 않고, 초기 목록은 REST로 곧바로 불러온다 (규칙 §2-1-2).
    // 조회를 WS 연결 성공에 묶어 두면 연결이 늦거나 실패할 때 "학생 0명"이 그대로 남는다
    private suspend fun enterWaitingRoom(roomId: Long) {
        observeRoomEvents(roomId)
        refreshParticipants(roomId)
    }

    private suspend fun emitSessionStarted(pin: String) {
        hasHandedOffToPlay = true
        _event.emit(WaitingEvent.SessionStarted(pin))
    }

    // 종료 사실을 상태로도 남긴다 — 대기실이 백스택에 있는 동안 발행한 event는 아무도 받지 못하므로
    // (규칙 §7 replay=0), 되돌아오는 순간 recheckStatus가 이 값을 보고 다시 내보낸다
    private suspend fun emitSessionFinished(finishedRoomId: Long) {
        _uiState.update { it.copy(isLoading = false, isSessionFinished = true) }
        _event.emit(WaitingEvent.SessionFinished(finishedRoomId))
    }

    // 초기 목록·재접속 복구는 REST 조회, 이후 증분은 WS 이벤트 (규칙 §2-1-2)
    private fun observeRoomEvents(roomId: Long) {
        eventsJob?.cancel()
        eventsJob = viewModelScope.launch {
            sessionEventStream.events(roomId).collect { streamEvent ->
                when (streamEvent) {
                    is SessionEventStream.StreamEvent.Connected -> onConnected(roomId)
                    is SessionEventStream.StreamEvent.Received -> handleServerEvent(streamEvent.frame.event)
                    // 끊긴 동안은 참가·퇴장 증분이 오지 않아 화면의 인원이 사실과 달라진다.
                    // 재연결되면 Connected가 다시 조회한다 — 그전까지는 화면이 알 수 있게 표시한다
                    is SessionEventStream.StreamEvent.Disconnected -> onDisconnected()
                }
            }
        }
    }

    // 연결(재연결) 직후 — M-07 오버레이를 내리고 명단을 REST로 다시 맞춘다
    private suspend fun onConnected(roomId: Long) {
        _uiState.update { it.copy(isDisconnected = false) }
        refreshParticipants(roomId)
    }

    // M-07 오버레이를 띄우고, 명단이 낡았다는 표시도 함께 남긴다 —
    // 오버레이는 재연결로만 내려가지만 그 뒤 조회가 실패하면 화면이 재시도를 걸어야 한다
    private fun onDisconnected() {
        _uiState.update { it.copy(isDisconnected = true, hasParticipantsError = true) }
    }

    // M-07 "지금 다시 연결" — 재연결 백오프를 기다리지 않고 즉시 다시 구독한다.
    // 스트림이 다시 붙으면 Connected가 와서 오버레이가 내려간다
    private fun onReconnect() {
        val currentRoomId = roomId

        if (currentRoomId != null) {
            observeRoomEvents(currentRoomId)
        }
    }

    private fun onRetryParticipants() {
        val currentRoomId = roomId

        if (currentRoomId != null) {
            viewModelScope.launch { refreshParticipants(currentRoomId) }
        }
    }

    private suspend fun refreshParticipants(roomId: Long) {
        _uiState.update { it.copy(isParticipantsLoading = true, hasParticipantsError = false) }
        getParticipantsUseCase.invoke(roomId)
            .onSuccess { participants ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isParticipantsLoading = false,
                        hasParticipantsError = false,
                        participants = participants,
                        totalCount = participants.size
                    )
                }
            }
            .onFailure {
                // 조용히 0명으로 두면 조회 실패가 "학생 0명이 함께해요"로 보인다 — 화면이 재시도를 걸게 알린다
                _uiState.update { it.copy(isParticipantsLoading = false, hasParticipantsError = true) }
            }
    }

    private suspend fun handleServerEvent(event: ServerEvent) {
        when (event) {
            is ServerEvent.ParticipantJoined -> onParticipantJoined(event)
            is ServerEvent.ParticipantLeft -> onParticipantLeft(event)
            is ServerEvent.SessionStarted -> emitSessionStarted(_uiState.value.pin)
            // 대기실에 있는 동안 세션이 끝났다 (선생님이 시작 없이 종료한 경우 포함)
            is ServerEvent.SessionEnded -> onSessionEnded()
            is ServerEvent.RoomCancelled -> _event.emit(WaitingEvent.RoomClosed("방이 취소됐어요"))
            else -> Unit
        }
    }

    private suspend fun onSessionEnded() {
        val endedRoomId = roomId

        if (hasHandedOffToPlay || endedRoomId == null) {
            // 풀이 화면이 앞에 있다 — 사용자가 보고 있는 최종 순위(M-05)를 가로채지 않고 상태만 남긴다.
            // 뒤로가기로 대기실에 돌아오는 순간 recheckStatus가 이 값을 보고 결과로 보낸다
            _uiState.update { it.copy(isSessionFinished = true) }
        } else {
            emitSessionFinished(endedRoomId)
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

        // 서버가 현재 인원을 안 실어 준다 — 명단 길이로 센다
        _uiState.update { state ->
            val others = state.participants.filter { it.participantId != joined.participantId }
            val participants = others + joined

            state.copy(
                participants = participants,
                totalCount = participants.size
            )
        }
    }

    private suspend fun onParticipantLeft(event: ServerEvent.ParticipantLeft) {
        val isMe = event.participantId == _uiState.value.myParticipantId

        if (isMe && event.reason == ServerEvent.ParticipantLeft.REASON_KICKED) {
            _event.emit(WaitingEvent.RoomClosed("선생님이 내보냈어요"))
        } else {
            _uiState.update { state ->
                val participants = state.participants.filter { it.participantId != event.participantId }

                state.copy(
                    participants = participants,
                    totalCount = participants.size
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
            is WaitingAction.RetryParticipants -> onRetryParticipants()
            is WaitingAction.ClickLeave -> onClickLeave()
            is WaitingAction.Reconnect -> onReconnect()
        }
    }
}
