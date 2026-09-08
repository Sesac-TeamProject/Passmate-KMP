import Shared

struct WaitingUiState {
    var isLoading: Bool = true

    var roomTitle: String = ""

    var pin: String = ""

    var myParticipantId: Int64?

    var myNickname: String?

    var participants: [Participant] = []

    var totalCount: Int = 0

    // STOMP가 끊긴 동안 true — 컨테이너가 M-07 연결 끊김 오버레이를 띄운다
    var isDisconnected: Bool = false
}
