enum SessionControlAction {
    case enter(roomId: Int64, pin: String)
    case retry
    case clickStart
    case clickNext
    case clickEndQuestion
    // 확인 다이얼로그를 거친 뒤 호출된다 — 다이얼로그 소유는 화면 (규칙 §11-1)
    case confirmEndSession
    case toggleLock
    // PTT 녹음 종료 → 클립 업로드 (M-T2 "길게 눌러 힌트 말하기", T121)
    case sendVoiceHint(hint: RecordedVoiceHint)
    case notice(message: String)
}
