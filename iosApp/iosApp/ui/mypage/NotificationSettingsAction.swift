// 알림 항목 3종 (M-12-10) — 세션 시작·별점 요청·정산 완료 (Compose NotificationKind 미러)
enum NotificationKind {
    case sessionStart
    case ratingRequest
    case settlementDone
}

enum NotificationSettingsAction {
    case enter
    case retry
    // 토글 즉시 저장 — 실패 시 원복
    case toggle(kind: NotificationKind)
}
