import Shared

struct HostedRoomsUiState {
    var isLoading: Bool = true

    var loadFailed: Bool = false

    var grade: MyGrade?

    var ongoing: [HostedRoom] = []

    var ended: [HostedRoom] = []

    var nextCursor: String?

    var isLoadingMore: Bool = false
}
