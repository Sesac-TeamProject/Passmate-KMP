import Shared

struct SessionControlUiState {
    var isLoading: Bool = true

    var loadFailed: Bool = false

    var roomTitle: String = ""

    var pin: String = ""

    var status: RoomStatus = RoomStatus.waiting

    var participantCount: Int = 0

    var questionCount: Int?

    var question: SessionQuestion?

    // 서버 endsAt 기반 렌더링 전용 초 카운트 — 마감 판정은 서버가 한다 (규칙 §1·§5)
    var remainingSec: Int = 0

    var isQuestionClosed: Bool = false

    var submissions: SubmissionStatus?

    var isLocked: Bool = false

    var isProjectorConnected: Bool = false

    // 제어 요청 in-flight — 중복 호출 방지 (규칙 §9)
    var isControlling: Bool = false
}
