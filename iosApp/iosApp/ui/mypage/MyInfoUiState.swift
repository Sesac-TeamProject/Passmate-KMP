import Shared

struct MyInfoUiState {
    var isLoading: Bool = true

    var loadFailed: Bool = false

    var summary: MyPageSummary?

    var ongoing: OngoingRoom?

    var rooms: [JoinedRoom] = []

    var nextCursor: String?

    var isLoadingMore: Bool = false
}
