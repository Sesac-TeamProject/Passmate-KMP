import Shared

// 공개 방 목록·탐색 (M-11) — Compose RoomListUiState.kt 미러
struct RoomListUiState {
    var isLoading: Bool = true

    var isLoadingMore: Bool = false

    var rooms: [PublicRoom] = []

    var query: String = ""

    var typeFilter: RoomTypeFilter = .all

    var hasNext: Bool = false

    var nextCursor: String? = nil

    var hasError: Bool = false

    var isEmpty: Bool {
        !isLoading && !hasError && rooms.isEmpty
    }
}
