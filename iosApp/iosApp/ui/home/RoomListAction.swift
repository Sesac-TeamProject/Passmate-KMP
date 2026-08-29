import Shared

enum RoomListAction {
    case changeQuery(query: String)
    case submitSearch
    case selectType(type: RoomTypeFilter)
    case clickRoom(pin: String)
    // 선생님 이름 탭 → 프로필 시트 (M-10)
    case clickHost(hostId: Int64)
    case loadMore
    case retry
    case clickPinEntry
    // 프로필 시트(M-10) 등 화면 위 계층에서 올라온 안내 문구 — 토스트로 노출
    case notice(message: String)
}
