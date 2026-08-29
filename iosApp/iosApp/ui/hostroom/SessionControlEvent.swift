enum SessionControlEvent {
    // 리모컨은 호스트(회원) 전용 — 게스트 진입 시 로그인 유도 (규칙 §8)
    case requireSignIn
    // SESSION_ENDED 수신 → 방 리포트로 이동 (세션 플로우 전환은 서버 이벤트로만, 규칙 §2-1-2)
    case sessionEnded(roomId: Int64)
    case showNotice(message: String)
}
