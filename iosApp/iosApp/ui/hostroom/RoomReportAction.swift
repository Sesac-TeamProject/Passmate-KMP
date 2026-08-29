enum RoomReportAction {
    case enter(roomId: Int64)
    case retry(roomId: Int64)
    case selectTab(tab: ReportTab)
    // 내보내기 — 텍스트 요약 네이티브 공유 (FR-063 모바일 경로)
    case clickExport
}
