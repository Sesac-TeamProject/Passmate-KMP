enum SessionControlAction {
    case enter(roomId: Int64, pin: String)
    case retry
    case clickStart
    case clickNext
    case clickEndQuestion
    // 확인 다이얼로그를 거친 뒤 호출된다 — 다이얼로그 소유는 화면 (규칙 §11-1)
    case confirmEndSession
    case toggleLock
}
