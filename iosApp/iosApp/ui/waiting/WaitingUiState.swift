import Shared

struct WaitingUiState {
    var isLoading: Bool = true

    var roomTitle: String = ""

    var pin: String = ""

    var myParticipantId: Int64?

    var myNickname: String?

    var participants: [Participant] = []

    var totalCount: Int = 0

    // 세션 종료는 상태로도 남긴다 — 대기실이 스택에 있는 동안 보낸 event는 아무도 받지 못한다 (규칙 §7)
    var isSessionFinished: Bool = false
}
