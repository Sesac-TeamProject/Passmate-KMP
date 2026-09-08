package org.sesacteamproject.passmate.ui.play

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.onFailure
import org.sesacteamproject.passmate.core.model.onSuccess
import org.sesacteamproject.passmate.core.network.SessionEventStream
import org.sesacteamproject.passmate.core.network.event.ServerEvent
import org.sesacteamproject.passmate.core.network.event.ServerEventFrame
import org.sesacteamproject.passmate.mvi.MviViewModel
import org.sesacteamproject.passmate.room.domain.model.RoomStatus
import org.sesacteamproject.passmate.room.domain.usecase.GetMyParticipationUseCase
import org.sesacteamproject.passmate.room.domain.usecase.GetRoomInfoUseCase
import org.sesacteamproject.passmate.room.domain.usecase.LeaveRoomUseCase
import org.sesacteamproject.passmate.session.domain.model.AnswerResult
import org.sesacteamproject.passmate.session.domain.model.QuestionDeadline
import org.sesacteamproject.passmate.session.domain.model.QuestionType
import org.sesacteamproject.passmate.session.domain.model.RankEntry
import org.sesacteamproject.passmate.session.domain.model.SessionQuestion
import org.sesacteamproject.passmate.session.domain.model.distributionOf
import org.sesacteamproject.passmate.session.domain.model.SessionSnapshot
import org.sesacteamproject.passmate.session.domain.model.VoiceHint
import org.sesacteamproject.passmate.session.domain.policy.SnapshotPolicy
import org.sesacteamproject.passmate.session.domain.usecase.GetSessionSnapshotUseCase
import org.sesacteamproject.passmate.session.domain.usecase.GetVoiceHintsUseCase
import org.sesacteamproject.passmate.session.domain.usecase.SubmitAnswerUseCase
import org.sesacteamproject.passmate.user.domain.usecase.RequestGuestClaimUseCase

class PlayViewModel(
    private val getRoomInfoUseCase: GetRoomInfoUseCase,
    private val getSessionSnapshotUseCase: GetSessionSnapshotUseCase,
    private val submitAnswerUseCase: SubmitAnswerUseCase,
    private val getVoiceHintsUseCase: GetVoiceHintsUseCase,
    private val leaveRoomUseCase: LeaveRoomUseCase,
    private val getMyParticipationUseCase: GetMyParticipationUseCase,
    private val requestGuestClaimUseCase: RequestGuestClaimUseCase,
    private val snapshotPolicy: SnapshotPolicy,
    private val sessionEventStream: SessionEventStream
) : MviViewModel<PlayUiState, PlayAction, PlayEvent>(PlayUiState()) {

    private var roomId: Long? = null

    private var snapshotTs: String? = null

    private var deadline: QuestionDeadline? = null

    private var eventsJob: Job? = null

    private var tickerJob: Job? = null

    private fun onEnter(pin: String) {
        if (roomId != null) {
            return
        }
        val my = getMyParticipationUseCase.invoke()

        _uiState.update {
            it.copy(
                myParticipantId = my?.participantId,
                myNickname = my?.nickname,
                isGuest = my?.isGuest ?: false
            )
        }
        viewModelScope.launch {
            getRoomInfoUseCase.invoke(pin)
                .onSuccess { room ->
                    roomId = room.roomId
                    observeRoomEvents(room.roomId)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _event.emit(PlayEvent.RoomClosed(errorMessage(error)))
                }
        }
    }

    private fun observeRoomEvents(roomId: Long) {
        eventsJob?.cancel()
        eventsJob = viewModelScope.launch {
            sessionEventStream.events(roomId).collect { streamEvent ->
                when (streamEvent) {
                    is SessionEventStream.StreamEvent.Connected -> onConnected(roomId)
                    is SessionEventStream.StreamEvent.Received -> handleFrame(streamEvent.frame)
                    is SessionEventStream.StreamEvent.Disconnected -> _uiState.update { it.copy(isDisconnected = true) }
                }
            }
        }
    }

    // 재접속·늦은 입장 복구 — 스냅샷 적용 후 이후 이벤트만 증분 반영 (규칙 §2-1-2)
    // 연결(재연결) 직후 — M-07 오버레이를 내리고 스냅샷으로 진행 중인 문항에 복귀한다 (규칙 §2-1-2)
    private suspend fun onConnected(roomId: Long) {
        _uiState.update { it.copy(isDisconnected = false) }
        loadSnapshot(roomId)
    }

    // M-07 "지금 다시 연결" — 재연결 백오프를 기다리지 않고 즉시 다시 구독한다.
    // 스트림이 다시 붙으면 Connected가 와서 오버레이가 내려간다
    private fun onReconnect() {
        val currentRoomId = roomId

        if (currentRoomId != null) {
            observeRoomEvents(currentRoomId)
        }
    }

    private suspend fun loadSnapshot(roomId: Long) {
        getSessionSnapshotUseCase.invoke(roomId)
            .onSuccess { snapshot ->
                snapshotTs = snapshot.ts
                applySnapshot(snapshot)
                restoreVoiceHint(roomId, snapshot.currentQuestion?.questionNo)
            }
            .onFailure {
                _uiState.update { state -> state.copy(isLoading = false) }
            }
    }

    // 재접속 시 현재 문항의 마지막 힌트를 복구한다 — 자동 재생 없이 다시 듣기만 (FR-041)
    private suspend fun restoreVoiceHint(roomId: Long, currentQuestionNo: Int?) {
        if (currentQuestionNo != null) {
            getVoiceHintsUseCase.invoke(roomId).onSuccess { hints ->
                val latest = hints.lastOrNull { it.questionNo == currentQuestionNo }

                _uiState.update { it.copy(activeVoiceHint = latest) }
            }
        }
    }

    private fun applySnapshot(snapshot: SessionSnapshot) {
        val question = snapshot.currentQuestion
        val myAnswer = question?.let { current ->
            snapshot.myAnswers.firstOrNull { it.questionId == current.questionId }
        }

        deadline = if (question != null && !question.isClosed) {
            QuestionDeadline.fromServerTimes(question.endsAt, snapshot.ts)
        } else {
            null
        }
        _uiState.update { state ->
            state.copy(
                isLoading = false,
                phase = when {
                    snapshot.status == RoomStatus.FINISHED -> PlayUiState.Phase.FINISHED
                    question != null -> PlayUiState.Phase.QUESTION
                    else -> PlayUiState.Phase.IDLE
                },
                questionCount = snapshot.questionCount ?: state.questionCount,
                question = question,
                selectedChoiceIndex = null,
                essayAnswer = "",
                hasSubmitted = myAnswer != null,
                myAnswerResult = null,
                reveal = null,
                totalScore = snapshot.totalScore ?: state.totalScore,
                myCorrectCount = snapshot.myAnswers.count { it.correct == true },
                rank = snapshot.rank ?: state.rank,
                ranking = snapshot.ranking,
                finalRanking = if (snapshot.status == RoomStatus.FINISHED) snapshot.ranking else state.finalRanking,
                isLocked = snapshot.isLocked
            )
        }
        restartTicker()
    }

    private suspend fun handleFrame(frame: ServerEventFrame) {
        val staleAgainst = snapshotTs

        if (staleAgainst != null && snapshotPolicy.isStaleFrame(frame.ts, staleAgainst)) {
            return
        }
        when (val event = frame.event) {
            is ServerEvent.QuestionStarted -> onQuestionStarted(event, frame.ts)
            is ServerEvent.QuestionEnded -> onQuestionEnded(event)
            is ServerEvent.RankingUpdated -> _uiState.update { it.copy(ranking = event.ranking.map(::toRankEntry)) }
            is ServerEvent.ScreenLocked -> _uiState.update { it.copy(isLocked = event.locked) }
            // 서버는 SESSION_STARTED에 페이로드를 싣지 않는다 — 문항 수는 스냅샷 값을 유지한다
            is ServerEvent.SessionStarted -> _uiState.update { it.copy(isLoading = false) }
            is ServerEvent.SessionEnded -> onSessionEnded(event)
            is ServerEvent.HintPublished -> onHintPublished(event)
            is ServerEvent.RoomCancelled -> _event.emit(PlayEvent.RoomClosed("방이 취소됐어요"))
            is ServerEvent.ParticipantLeft -> onParticipantLeft(event)
            else -> Unit
        }
    }

    // 수신 즉시 자동 재생(FR-040, 3초 SLA) — 재생 실패 시 배너의 수동 재생으로 폴백된다
    private suspend fun onHintPublished(event: ServerEvent.HintPublished) {
        val hint = VoiceHint(
            hintId = event.hintId,
            questionNo = event.questionNo,
            clipUrl = event.clipUrl,
            durationMs = event.durationMs
        )

        _uiState.update { it.copy(activeVoiceHint = hint) }
        _event.emit(PlayEvent.PlayVoiceHint(hint))
    }

    private fun onQuestionStarted(event: ServerEvent.QuestionStarted, serverTs: String) {
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

        deadline = QuestionDeadline.fromServerTimes(event.endsAt, serverTs)
        _uiState.update { state ->
            state.copy(
                isLoading = false,
                phase = PlayUiState.Phase.QUESTION,
                question = question,
                selectedChoiceIndex = null,
                essayAnswer = "",
                remainingSeconds = deadline?.remainingSeconds() ?: event.timeLimitSec,
                isSubmitting = false,
                hasSubmitted = false,
                myAnswerResult = null,
                reveal = null,
                activeVoiceHint = null
            )
        }
        restartTicker()
    }

    private fun onQuestionEnded(event: ServerEvent.QuestionEnded) {
        deadline = null
        tickerJob?.cancel()
        _uiState.update { state ->
            state.copy(
                phase = PlayUiState.Phase.IDLE,
                remainingSeconds = 0,
                reveal = PlayUiState.Reveal(
                    answer = event.answerReveal.answer,
                    explanation = event.answerReveal.explanation,
                    correctAnswererCount = event.correctCount,
                    distribution = state.question?.distributionOf(
                        raw = event.answerReveal.distribution,
                        answer = event.answerReveal.answer,
                        myChoiceIndex = state.selectedChoiceIndex
                    ).orEmpty()
                )
            )
        }
    }

    private fun onSessionEnded(event: ServerEvent.SessionEnded) {
        deadline = null
        tickerJob?.cancel()
        _uiState.update { state ->
            val finalRanking = event.finalRanking.map(::toRankEntry)
            val myEntry = finalRanking.firstOrNull { it.participantId == state.myParticipantId }

            state.copy(
                isLoading = false,
                phase = PlayUiState.Phase.FINISHED,
                finalRanking = finalRanking,
                rank = myEntry?.rank ?: state.rank,
                totalScore = myEntry?.total ?: state.totalScore
            )
        }
    }

    private suspend fun onParticipantLeft(event: ServerEvent.ParticipantLeft) {
        val isMe = event.participantId == _uiState.value.myParticipantId

        if (isMe && event.reason == ServerEvent.ParticipantLeft.REASON_KICKED) {
            _event.emit(PlayEvent.RoomClosed("선생님이 내보냈어요"))
        }
    }

    private fun restartTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive && deadline != null) {
                _uiState.update { it.copy(remainingSeconds = deadline?.remainingSeconds() ?: 0) }
                delay(TICKER_INTERVAL_MS)
            }
        }
    }

    private fun onSelectChoice(index: Int) {
        if (!_uiState.value.hasSubmitted && !_uiState.value.isLocked) {
            _uiState.update { it.copy(selectedChoiceIndex = index) }
        }
    }

    private fun onChangeEssayAnswer(text: String) {
        if (!_uiState.value.hasSubmitted) {
            _uiState.update { it.copy(essayAnswer = text) }
        }
    }

    private fun buildAnswerContent(state: PlayUiState): String? {
        val question = state.question ?: return null

        return when (question.type) {
            QuestionType.ESSAY -> state.essayAnswer.trim().ifEmpty { null }
            // OX 포함 — answerChoices가 서버가 안 주는 O/X를 채운다 (도메인 단일 출처).
            // 화면도 같은 목록으로 그리므로(PlayScreen) 눌린 보기와 보낸 값이 어긋나지 않는다
            else -> state.selectedChoiceIndex?.let { question.answerChoices.getOrNull(it) }
        }
    }

    // 클라이언트 가드는 UX 목적 — 마감·중복의 최종 판정은 서버(410·409)가 한다 (규칙 §1)
    private fun onClickSubmit() {
        val state = _uiState.value
        val currentRoomId = roomId
        val question = state.question
        val content = buildAnswerContent(state)

        if (state.isSubmitting || state.hasSubmitted || currentRoomId == null || question == null) {
            return
        }
        viewModelScope.launch {
            if (state.isLocked) {
                _event.emit(PlayEvent.ShowNotice("선생님이 화면을 잠갔어요"))
            } else if (content == null) {
                _event.emit(PlayEvent.ShowNotice("답을 선택하거나 입력해 주세요"))
            } else {
                _uiState.update { it.copy(isSubmitting = true) }
                submitAnswer(currentRoomId, question.questionId, content)
            }
        }
    }

    private suspend fun submitAnswer(roomId: Long, questionId: Long, content: String) {
        submitAnswerUseCase.invoke(roomId, questionId, content)
            .onSuccess { result -> applyAnswerResult(result) }
            .onFailure { error ->
                _uiState.update { it.copy(isSubmitting = false) }
                handleSubmitFailure(error)
            }
    }

    private fun applyAnswerResult(result: AnswerResult) {
        _uiState.update { state ->
            state.copy(
                isSubmitting = false,
                hasSubmitted = true,
                myAnswerResult = result,
                totalScore = result.totalScore,
                myCorrectCount = if (result.correct == true) state.myCorrectCount + 1 else state.myCorrectCount,
                rank = result.rank ?: state.rank
            )
        }
    }

    // 서버는 마감(QUESTION_NOT_RUNNING)·중복(ALREADY_SUBMITTED)·잠금(SCREEN_LOCKED)을 모두 409로 준다.
    // code로 갈라야 문구가 맞고, 잠금은 풀리면 다시 낼 수 있으므로 hasSubmitted를 세우지 않는다 (규칙 §10)
    private suspend fun handleSubmitFailure(error: AppError) {
        val code = error.serverCode
        val isClosed = error is AppError.Gone || (error is AppError.Conflict && code == "QUESTION_NOT_RUNNING")

        if (error is AppError.Conflict && code == "SCREEN_LOCKED") {
            _event.emit(PlayEvent.ShowNotice("선생님이 화면을 잠갔어요"))
        } else if (isClosed) {
            _uiState.update { it.copy(hasSubmitted = true) }
            _event.emit(PlayEvent.ShowNotice("이미 마감된 문항이에요"))
        } else if (error is AppError.Conflict) {
            _uiState.update { it.copy(hasSubmitted = true) }
            _event.emit(PlayEvent.ShowNotice("이미 제출한 문항이에요"))
        } else {
            _event.emit(PlayEvent.ShowNotice(errorMessage(error)))
        }
    }

    private fun onConfirmLeave() {
        val leavingRoomId = roomId

        deadline = null
        tickerJob?.cancel()
        eventsJob?.cancel()
        viewModelScope.launch {
            if (leavingRoomId != null) {
                leaveRoomUseCase.invoke(leavingRoomId)
            }
            _event.emit(PlayEvent.Left)
        }
    }

    private fun onClickReplayHint() {
        val hint = _uiState.value.activeVoiceHint

        if (hint != null) {
            viewModelScope.launch {
                _event.emit(PlayEvent.PlayVoiceHint(hint))
            }
        }
    }

    private fun onClickViewReport() {
        val currentRoomId = roomId

        if (currentRoomId != null) {
            viewModelScope.launch {
                _event.emit(PlayEvent.OpenResult(currentRoomId))
            }
        }
    }

    // 게스트 가입 유도 (T075) — participantId를 대기 큐에 넣고 로그인 화면으로
    private fun onClickSignup() {
        val participantId = _uiState.value.myParticipantId

        viewModelScope.launch {
            if (participantId != null) {
                requestGuestClaimUseCase.invoke(participantId)
            }
            _event.emit(PlayEvent.OpenSignup)
        }
    }

    private fun toRankEntry(entry: ServerEvent.RankingEntry): RankEntry {
        return RankEntry(
            rank = entry.rank,
            participantId = entry.participantId,
            nickname = entry.nickname,
            avatarId = entry.avatarId,
            total = entry.total
        )
    }

    // 방·문항·참가자를 서버가 전부 404로 준다 — code로 갈라야 무엇이 없는지 화면이 말해 줄 수 있다 (규칙 §10)
    private fun notFoundMessage(serverCode: String?): String {
        return when (serverCode) {
            "PARTICIPANT_NOT_FOUND" -> "이 방에 입장한 기록이 없어요. 다시 입장해 주세요"
            "QUESTION_NOT_FOUND" -> "이 방에 없는 문항이에요"
            "QUESTION_SET_NOT_FOUND" -> "방의 문제 세트를 찾을 수 없어요"
            else -> "방을 찾을 수 없어요"
        }
    }

    private fun errorMessage(error: AppError): String {
        return when (error) {
            is AppError.NotFound -> notFoundMessage(error.serverCode)
            is AppError.Gone -> "이미 종료된 방이에요"
            is AppError.PermissionDenied -> "이 방에 입장한 사람만 답을 낼 수 있어요"
            is AppError.NetworkError -> "네트워크 연결을 확인해 주세요"
            else -> "요청에 실패했어요. 잠시 후 다시 시도해 주세요"
        }
    }

    override fun onAction(action: PlayAction) {
        when (action) {
            is PlayAction.Enter -> onEnter(action.pin)
            is PlayAction.SelectChoice -> onSelectChoice(action.index)
            is PlayAction.ChangeEssayAnswer -> onChangeEssayAnswer(action.text)
            is PlayAction.ClickSubmit -> onClickSubmit()
            is PlayAction.ClickReplayHint -> onClickReplayHint()
            is PlayAction.ClickSignup -> onClickSignup()
            is PlayAction.ConfirmLeave -> onConfirmLeave()
            is PlayAction.ClickViewReport -> onClickViewReport()
            is PlayAction.Reconnect -> onReconnect()
        }
    }

    companion object {
        private const val TICKER_INTERVAL_MS = 200L
    }
}
