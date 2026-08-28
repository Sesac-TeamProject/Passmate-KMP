import Shared

enum RoomListAction {
    case changeQuery(query: String)
    case submitSearch
    case selectType(type: RoomTypeFilter)
    case clickRoom(pin: String)
    case loadMore
    case retry
    case clickPinEntry
}
