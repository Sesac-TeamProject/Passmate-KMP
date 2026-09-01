package org.sesacteamproject.passmate.ui.hostroom

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.auth.domain.usecase.IsSignedInUseCase
import org.sesacteamproject.passmate.core.model.onFailure
import org.sesacteamproject.passmate.core.model.onSuccess
import org.sesacteamproject.passmate.mvi.MviViewModel
import org.sesacteamproject.passmate.report.domain.usecase.BuildRoomReportSummaryUseCase
import org.sesacteamproject.passmate.report.domain.usecase.GetRoomReportUseCase

class RoomReportViewModel(
    private val getRoomReportUseCase: GetRoomReportUseCase,
    private val buildRoomReportSummaryUseCase: BuildRoomReportSummaryUseCase,
    private val isSignedInUseCase: IsSignedInUseCase
) : MviViewModel<RoomReportUiState, RoomReportAction, RoomReportEvent>(RoomReportUiState()) {

    private var hasEntered = false

    private fun onEnter(roomId: Long) {
        if (hasEntered) {
            return
        }
        hasEntered = true
        // 호스트(회원) 전용 가드 — 서버 403이 최종 권위지만 UX상 진입 시 먼저 로그인 유도한다 (규칙 §8)
        if (!isSignedInUseCase.invoke()) {
            viewModelScope.launch {
                _event.emit(RoomReportEvent.RequireSignIn)
            }
        } else {
            load(roomId)
        }
    }

    private fun load(roomId: Long) {
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            getRoomReportUseCase.invoke(roomId)
                .onSuccess { report ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadFailed = false,
                            report = report
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false, loadFailed = true) }
                }
        }
    }

    private fun onSelectTab(tab: ReportTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    private fun onSelectStudentSort(sort: StudentSort) {
        _uiState.update { it.copy(studentSort = sort) }
    }

    private fun onClickExport() {
        val report = _uiState.value.report

        if (report != null) {
            val summary = buildRoomReportSummaryUseCase.invoke(report)

            viewModelScope.launch {
                _event.emit(RoomReportEvent.ShareReport(summary))
            }
        }
    }

    override fun onAction(action: RoomReportAction) {
        when (action) {
            is RoomReportAction.Enter -> onEnter(action.roomId)
            is RoomReportAction.Retry -> load(action.roomId)
            is RoomReportAction.SelectTab -> onSelectTab(action.tab)
            is RoomReportAction.SelectStudentSort -> onSelectStudentSort(action.sort)
            is RoomReportAction.ClickExport -> onClickExport()
        }
    }
}
