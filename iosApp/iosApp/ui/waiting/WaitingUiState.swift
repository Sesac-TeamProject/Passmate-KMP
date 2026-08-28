import Shared

struct WaitingUiState {
    var isLoading: Bool = true

    var roomTitle: String = ""

    var pin: String = ""

    var myParticipantId: Int64?

    var myNickname: String?

    var participants: [Participant] = []

    var totalCount: Int = 0
}
