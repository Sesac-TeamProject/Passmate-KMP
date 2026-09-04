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
                    is SessionEventStream.StreamEvent.Connected -> loadSnapshot(roomId)
                    is SessionEventStream.StreamEvent.Received -> handleFrame(streamEvent.frame)
                    is SessionEventStream.StreamEvent.Disconnected -> Unit
                }
            }
        }
    }

    // 재접속·늦은 입장 복구 — 스냅샷 적용 후 이후 이벤트만 증분 반영 (규칙 §2-1-2)
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
            QuestionType.OX -> when (state.selectedChoiceIndex) {
                0 -> "O"
                1 -> "X"
                else -> null
            }
            QuestionType.ESSAY -> state.essayAnswer.trim().ifEmpty { null }
            // OX는 서버가 choices를 주지 않아 answerChoices가 O/X를 채운다 (도메인 단일 출처)
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

    private suspend fun handleSubmitFailure(error: AppError) {
        when (error) {
            is AppError.Gone -> {
                _uiState.update { it.copy(hasSubmitted = true) }
                _event.emit(PlayEvent.ShowNotice("이미 마감된 문항이에요"))
            }
            is AppError.Conflict -> {
                _uiState.update { it.copy(hasSubmitted = true) }
                _event.emit(PlayEvent.ShowNotice("이미 제출한 문항이에요"))
            }
            else -> _event.emit(PlayEvent.ShowNotice(errorMessage(error)))
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

    private fun errorMessage(error: AppError): String {
        return when (error) {
            is AppError.NotFound -> "방을 찾을 수 없어요"
            is AppError.Gone -> "이미 종료된 방이에요"
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
        }
    }

    companion object {
        private const val TICKER_INTERVAL_MS = 200L
    }
}
