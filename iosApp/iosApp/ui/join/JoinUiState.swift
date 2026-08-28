import Shared

struct JoinUiState {
    var pin: String = ""

    var nickname: String = ""

    var avatarId: Int = StudentAvatars.defaultId

    var isJoining: Bool = false

    var isSignedIn: Bool = false

    // 입장 전 방 정보(호스트 등급·별점) — PIN 6자리 입력 시 프리페치 (T081)
    var roomInfo: RoomInfo?

    var isLoadingRoomInfo: Bool = false
}
