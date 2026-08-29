package org.sesacteamproject.passmate.ui.hostroom

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.auth.domain.usecase.IsSignedInUseCase
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.model.onFailure
import org.sesacteamproject.passmate.core.model.onSuccess
import org.sesacteamproject.passmate.core.network.SessionEventStream
import org.sesacteamproject.passmate.core.network.event.ServerEvent
import org.sesacteamproject.passmate.mvi.MviViewModel
import org.sesacteamproject.passmate.room.domain.model.RoomStatus
import org.sesacteamproject.passmate.room.domain.usecase.GetRoomInfoUseCase
import org.sesacteamproject.passmate.session.domain.model.QuestionDeadline
import org.sesacteamproject.passmate.session.domain.model.QuestionType
import org.sesacteamproject.passmate.session.domain.model.SessionQuestion
import org.sesacteamproject.passmate.session.domain.usecase.EndCurrentQuestionUseCase
import org.sesacteamproject.passmate.session.domain.usecase.EndSessionUseCase
import org.sesacteamproject.passmate.session.domain.usecase.GetSessionSnapshotUseCase
import org.sesacteamproject.passmate.session.domain.usecase.GetSubmissionsUseCase
import org.sesacteamproject.passmate.session.domain.usecase.NextQuestionUseCase
import org.sesacteamproject.passmate.session.domain.usecase.SetScreenLockUseCase
import org.sesacteamproject.passmate.session.domain.usecase.StartSessionUseCase

class SessionControlViewModel(
    private val getRoomInfoUseCase: GetRoomInfoUseCase,
    private val getSessionSnapshotUseCase: GetSessionSnapshotUseCase,
    private val getSubmissionsUseCase: GetSubmissionsUseCase,
    private val startSessionUseCase: StartSessionUseCase,
    private val nextQuestionUseCase: NextQuestionUseCase,
    private val endCurrentQuestionUseCase: EndCurrentQuestionUseCase,
    private val endSessionUseCase: EndSessionUseCase,
    private val setScreenLockUseCase: SetScreenLockUseCase,
    private val sessionEventStream: SessionEventStream,
    private val isSignedInUseCase: IsSignedInUseCase
) : MviViewModel<SessionControlUiState, SessionControlAction, SessionControlEvent>(SessionControlUiState()) {

    private var roomId = -1L

    private var pin = ""

    private var eventsJob: Job? = null

    private var timerJob: Job? = null

    private var deadline: QuestionDeadline? = null

    private fun onEnter(roomId: Long, pin: String) {
        if (this.roomId == roomId) {
            return
        }
        this.roomId = roomId
        this.pin = pin
        // 호스트(회원) 전용 가드 — 호스트 검증은 서버 403이 최종 권위 (규칙 §8)
        if (!isSignedInUseCase.invoke()) {
            viewModelScope.launch {
                _event.emit(SessionControlEvent.RequireSignIn)
            }
        } else {
            load()
            observeEvents()
        }
    }

    private fun load() {
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            val roomResult = getRoomInfoUseCase.invoke(pin)

            if (roomResult is AppResult.Success) {
                val room = roomResult.value

                _uiState.update {
                    it.copy(
                        roomTitle = room.title,
                        pin = room.pin,
                        participantCount = room.participantCount ?: 0,
                        questionCount = room.questionCount
                    )
                }
                refreshSnapshot(isFirstLoad = true)
            } else {
                _uiState.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    // 재접속 프로토콜(규칙 §2-1-2) — Connected 수신 시에도 호출해 스냅샷으로 상태를 복구한다
    private suspend fun refreshSnapshot(isFirstLoad: Boolean) {
        getSessionSnapshotUseCase.invoke(roomId)
            .onSuccess { snapshot ->
                val question = snapshot.currentQuestion

                deadline = question?.let { QuestionDeadline.fromServerTimes(it.endsAt, snapshot.ts) }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadFailed = false,
                        status = snapshot.status,
                        questionCount = snapshot.questionCount ?: it.questionCount,
                        question = question,
                        isQuestionClosed = question?.isClosed ?: false,
                        isLocked = snapshot.isLocked
                    )
                }
                startTimer()
                if (snapshot.status == RoomStatus.RUNNING && question != null) {
                    refreshSubmissions()
                }
            }
            .onFailure { error ->
                if (isFirstLoad) {
                    // 세션 미시작 방은 스냅샷이 없을 수 있다 — 대기(WAITING) 상태로 취급
                    if (error is AppError.NotFound) {
                        _uiState.update { it.copy(isLoading = false, loadFailed = false, status = RoomStatus.WAITING) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, loadFailed = true) }
                    }
                }
            }
    }

    private suspend fun refreshSubmissions() {
        getSubmissionsUseCase.invoke(roomId)
            .onSuccess { submissions ->
                _uiState.update { it.copy(submissions = submissions) }
            }
            .onFailure {
                // 제출 현황은 부가 정보 — 실패해도 리모컨 제어를 막지 않는다
            }
    }

    private fun observeEvents() {
        eventsJob?.cancel()
        eventsJob = viewModelScope.launch {
            sessionEventStream.events(roomId, isHost = true).collect { streamEvent ->
                when (streamEvent) {
                    is SessionEventStream.StreamEvent.Connected -> refreshSnapshot(isFirstLoad = false)
                    is SessionEventStream.StreamEvent.Disconnected -> Unit
                    is SessionEventStream.StreamEvent.Received -> handleServerEvent(
                        streamEvent.frame.event,
                        streamEvent.frame.ts
                    )
                }
            }
        }
    }

    private suspend fun handleServerEvent(event: ServerEvent, ts: String) {
        when (event) {
            is ServerEvent.SessionStarted -> _uiState.update {
                it.copy(status = RoomStatus.RUNNING, questionCount = event.questionCount)
            }
            is ServerEvent.QuestionStarted -> {
                val question = SessionQuestion(
                    questionId = event.questionId,
                    questionNo = event.questionNo,
                    type = QuestionType.from(event.type),
                    body = event.body,
                    choices = event.choices.orEmpty(),
                    points = event.points,
                    timeLimitSec = event.timeLimitSec,
                    endsAt = event.endsAt,
                    isClosed = false
                )

                deadline = QuestionDeadline.fromServerTimes(event.endsAt, ts)
                _uiState.update {
                    it.copy(
                        status = RoomStatus.RUNNING,
                        question = question,
                        isQuestionClosed = false,
                        submissions = null
                    )
                }
                startTimer()
                refreshSubmissions()
            }
            is ServerEvent.QuestionEnded -> {
                _uiState.update { it.copy(isQuestionClosed = true) }
                refreshSubmissions()
            }
            is ServerEvent.AnswerSubmitted -> refreshSubmissions()
            is ServerEvent.SubmissionUpdated -> refreshSubmissions()
            is ServerEvent.ScreenLocked -> _uiState.update { it.copy(isLocked = event.locked) }
            is ServerEvent.ParticipantJoined -> _uiState.update { it.copy(participantCount = event.count) }
            is ServerEvent.ParticipantLeft -> _uiState.update { it.copy(participantCount = event.count) }
            is ServerEvent.ProjectorConnected -> _uiState.update { it.copy(isProjectorConnected = true) }
            is ServerEvent.ProjectorDisconnected -> _uiState.update { it.copy(isProjectorConnected = false) }
            is ServerEvent.SessionEnded -> {
                _uiState.update { it.copy(status = RoomStatus.FINISHED) }
                _event.emit(SessionControlEvent.SessionEnded(roomId))
            }
            else -> Unit
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val remaining = deadline?.remainingSeconds() ?: 0

                _uiState.update { it.copy(remainingSec = remaining) }
                delay(1_000L)
            }
        }
    }

    private fun onClickStart() {
        runControl { startSessionUseCase.invoke(roomId) }
    }

    private fun onClickNext() {
        runControl { nextQuestionUseCase.invoke(roomId) }
    }

    private fun onClickEndQuestion() {
        runControl { endCurrentQuestionUseCase.invoke(roomId) }
    }

    private fun onConfirmEndSession() {
        runControl { endSessionUseCase.invoke(roomId) }
    }

    private fun onToggleLock() {
        val nextLocked = !_uiState.value.isLocked

        runControl {
            setScreenLockUseCase.invoke(roomId, nextLocked)
                .onSuccess {
                    // SCREEN_LOCKED 브로드캐스트가 최종 상태지만 UX상 낙관 반영한다
                    _uiState.update { it.copy(isLocked = nextLocked) }
                }
        }
    }

    // 제어 요청 공통 처리 — 상태 전이는 서버 브로드캐스트로만 반영한다 (규칙 §2-1-2)
    private fun runControl(block: suspend () -> AppResult<*>) {
        if (_uiState.value.isControlling) {
            return
        }
        _uiState.update { it.copy(isControlling = true) }
        viewModelScope.launch {
            block()
                .onFailure { error ->
                    _event.emit(SessionControlEvent.ShowNotice(controlFailMessage(error)))
                }
            _uiState.update { it.copy(isControlling = false) }
        }
    }

    // 서버 code 기반 문구 분기 (규칙 §10)
    private fun controlFailMessage(error: AppError): String {
        return if (error is AppError.Conflict) {
            "확정된 문제 세트를 먼저 연결해 주세요"
        } else if (error is AppError.PermissionDenied) {
            "방을 만든 선생님만 진행할 수 있어요"
        } else if (error is AppError.NetworkError) {
            "네트워크 연결을 확인해 주세요"
        } else {
            "요청을 처리하지 못했어요. 다시 시도해 주세요"
        }
    }

    override fun onAction(action: SessionControlAction) {
        when (action) {
            is SessionControlAction.Enter -> onEnter(action.roomId, action.pin)
            is SessionControlAction.Retry -> load()
            is SessionControlAction.ClickStart -> onClickStart()
            is SessionControlAction.ClickNext -> onClickNext()
            is SessionControlAction.ClickEndQuestion -> onClickEndQuestion()
            is SessionControlAction.ConfirmEndSession -> onConfirmEndSession()
            is SessionControlAction.ToggleLock -> onToggleLock()
        }
    }
}
