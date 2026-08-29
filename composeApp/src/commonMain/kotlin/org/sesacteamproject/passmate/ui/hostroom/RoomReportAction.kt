package org.sesacteamproject.passmate.ui.hostroom

sealed interface RoomReportAction {

    data class Enter(val roomId: Long) : RoomReportAction

    data class Retry(val roomId: Long) : RoomReportAction

    data class SelectTab(val tab: ReportTab) : RoomReportAction

    // 내보내기 — 텍스트 요약 네이티브 공유 (FR-063 모바일 경로)
    data object ClickExport : RoomReportAction
}
