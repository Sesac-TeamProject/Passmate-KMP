// 공통 라우트 규격 (규칙 §2-1-1) — 이름·인자는 3플랫폼 동일 유지. Compose Route.kt와 1:1
enum Route: Hashable {
    case home
    case roomList
    case signIn
    case join(pin: String?)
    case waiting(pin: String)
    case play(pin: String)
    case result(roomId: Int64)
    case myInfo
    case payment(pin: String)
    case coinHistory
    case settings
}
