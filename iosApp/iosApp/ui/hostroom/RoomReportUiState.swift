import Shared

// M-14 방 리포트 탭 — 개요 / 문항별 / 학생별 (Compose ReportTab 미러)
enum ReportTab: CaseIterable {
    case overview
    case questions
    case students

    var label: String {
        switch self {
        case .overview: return "개요"
        case .questions: return "문항별"
        case .students: return "학생별"
        }
    }
}

struct RoomReportUiState {
    var isLoading: Bool = true

    var loadFailed: Bool = false

    var report: RoomReport?

    var selectedTab: ReportTab = .questions
}
