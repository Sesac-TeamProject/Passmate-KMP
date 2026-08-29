package org.sesacteamproject.passmate.ui.hostroom

import org.sesacteamproject.passmate.report.domain.model.RoomReport

// M-14 방 리포트 탭 — 개요 / 문항별 / 학생별
enum class ReportTab(val label: String) {
    OVERVIEW("개요"),
    QUESTIONS("문항별"),
    STUDENTS("학생별")
}

data class RoomReportUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val report: RoomReport? = null,
    val selectedTab: ReportTab = ReportTab.QUESTIONS
)
