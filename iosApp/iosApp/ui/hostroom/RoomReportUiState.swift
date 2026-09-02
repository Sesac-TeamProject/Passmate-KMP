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

// 학생별 탭 정렬 — 서버는 순위·점수를 그대로 주고 나열 순서만 화면에서 바꾼다 (Compose StudentSort 미러)
enum StudentSort: CaseIterable {
    case score
    case name

    var label: String {
        switch self {
        case .score: return "점수순"
        case .name: return "이름순"
        }
    }
}

struct RoomReportUiState {
    var isLoading: Bool = true

    var loadFailed: Bool = false

    var report: RoomReport?

    var selectedTab: ReportTab = .questions

    var studentSort: StudentSort = .score
}
