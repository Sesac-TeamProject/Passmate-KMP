package org.sesacteamproject.passmate.ui.hostroom

import org.sesacteamproject.passmate.report.domain.model.RoomReport

// M-14 방 리포트 탭 — 개요 / 문항별 / 학생별
enum class ReportTab(val label: String) {
    OVERVIEW("개요"),
    QUESTIONS("문항별"),
    STUDENTS("학생별")
}

// 학생별 탭 정렬 — 서버는 순위·점수를 그대로 주고 나열 순서만 화면에서 바꾼다 (서버 권위 유지)
enum class StudentSort(val label: String) {
    SCORE("점수순"),
    NAME("이름순")
}

data class RoomReportUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val report: RoomReport? = null,
    val selectedTab: ReportTab = ReportTab.QUESTIONS,
    val studentSort: StudentSort = StudentSort.SCORE
)
