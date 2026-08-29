enum SettingsAction {
    case enter
    case retry
    // 계정 정보·캐릭터 변경 시트 (M-12-1·M-12-7)
    case clickEditProfile
    case clickPaymentMethod
    case clickNotifications
    case clickCoinHistory
    // 확인 다이얼로그를 거친 뒤 호출된다 — 다이얼로그 소유는 화면 (규칙 §11-1)
    case confirmSignOut
    case confirmDeleteAccount
    // 시트에서 프로필 저장 완료 — 카드 갱신
    case profileUpdated
    case notice(message: String)
}
