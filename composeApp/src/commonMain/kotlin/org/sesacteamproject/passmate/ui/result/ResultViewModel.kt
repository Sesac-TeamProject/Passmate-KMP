package org.sesacteamproject.passmate.ui.result

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.core.model.getOrNull
import org.sesacteamproject.passmate.core.model.onFailure
import org.sesacteamproject.passmate.core.model.onSuccess
import org.sesacteamproject.passmate.core.network.SessionEventStream
import org.sesacteamproject.passmate.core.network.event.ServerEvent
import org.sesacteamproject.passmate.mvi.MviViewModel
import org.sesacteamproject.passmate.report.domain.usecase.BuildReportSummaryUseCase
import org.sesacteamproject.passmate.report.domain.usecase.GetLearningReportUseCase
import org.sesacteamproject.passmate.report.domain.usecase.GetSessionResultUseCase
import org.sesacteamproject.passmate.room.domain.usecase.GetMyParticipationUseCase
import org.sesacteamproject.passmate.user.domain.usecase.RequestGuestClaimUseCase

class ResultViewModel(
    private val getSessionResultUseCase: GetSessionResultUseCase,
    private val getLearningReportUseCase: GetLearningReportUseCase,
    private val buildReportSummaryUseCase: BuildReportSummaryUseCase,
    private val getMyParticipationUseCase: GetMyParticipationUseCase,
    private val requestGuestClaimUseCase: RequestGuestClaimUseCase,
    private val sessionEventStream: SessionEventStream
) : MviViewModel<ResultUiState, ResultAction, ResultEvent>(ResultUiState()) {

    private var roomId: Long? = null

    private var eventsJob: Job? = null

    private fun onEnter(roomId: Long) {
        if (this.roomId != null) {
            return
        }
        this.roomId = roomId
        load(roomId)
        observeUpdates(roomId)
    }

    private fun load(roomId: Long) {
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            val reportResult = getLearningReportUseCase.invoke(roomId)

            getSessionResultUseCase.invoke(roomId)
                .onSuccess { result ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            loadFailed = false,
                            result = result,
                            report = reportResult.getOrNull(),
                            selectedQuestionNo = state.selectedQuestionNo ?: firstAiQuestionNo(result)
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false, loadFailed = true) }
                }
        }
    }

    // AI 분석/서술형 문항을 기본 펼침 대상으로 — 없으면 첫 문항
    private fun firstAiQuestionNo(result: org.sesacteamproject.passmate.report.domain.model.SessionResult): Int? {
        val aiQuestion = result.questions.firstOrNull { it.aiFeedback != null }

        return aiQuestion?.questionNo ?: result.questions.firstOrNull()?.questionNo
    }

    // AI 분석 완료·첨삭 도착·리포트 생성 시 결과를 다시 불러온다 (FR-027·035, SC-009)
    private fun observeUpdates(roomId: Long) {
        eventsJob?.cancel()
        eventsJob = viewModelScope.launch {
            sessionEventStream.events(roomId).collect { streamEvent ->
                if (streamEvent is SessionEventStream.StreamEvent.Received) {
                    when (streamEvent.frame.event) {
                        is ServerEvent.FeedbackReady,
                        is ServerEvent.FeedbackFailed,
                        is ServerEvent.ReviewReceived,
                        is ServerEvent.ReportReady -> reload(roomId)
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun reload(roomId: Long) {
        viewModelScope.launch {
            val reportResult = getLearningReportUseCase.invoke(roomId)

            getSessionResultUseCase.invoke(roomId).onSuccess { result ->
                _uiState.update { state ->
                    state.copy(
                        result = result,
                        report = reportResult.getOrNull() ?: state.report
                    )
                }
            }
        }
    }

    private fun onSelectQuestion(questionNo: Int) {
        _uiState.update { state ->
            val next = if (state.selectedQuestionNo == questionNo) null else questionNo

            state.copy(selectedQuestionNo = next)
        }
    }

    private fun onClickExport() {
        val state = _uiState.value
        val result = state.result

        if (result == null || state.isSharing) {
            return
        }
        val summary = buildReportSummaryUseCase.invoke(result, state.report)

        viewModelScope.launch {
            _event.emit(ResultEvent.ShareReport(summary))
        }
    }

    private fun onRetry() {
        val currentRoomId = roomId

        if (currentRoomId != null) {
            load(currentRoomId)
        }
    }

    // 게스트 가입 유도 — participantId를 대기 큐에 넣고 로그인 화면으로 (로그인 완료 후 claim, FR-036)
    private fun onClickSignup() {
        val participation = getMyParticipationUseCase.invoke()

        viewModelScope.launch {
            if (participation != null) {
                requestGuestClaimUseCase.invoke(participation.participantId)
                _event.emit(ResultEvent.NavigateToSignup)
            } else {
                _event.emit(ResultEvent.NavigateToSignup)
            }
        }
    }

    override fun onAction(action: ResultAction) {
        when (action) {
            is ResultAction.Enter -> onEnter(action.roomId)
            is ResultAction.SelectQuestion -> onSelectQuestion(action.questionNo)
            is ResultAction.ClickExport -> onClickExport()
            is ResultAction.ClickSignup -> onClickSignup()
            is ResultAction.Retry -> onRetry()
        }
    }
}
