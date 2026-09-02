import Foundation

enum MyInfoEvent {
    // 마이는 회원 전용 — 딥링크 직접 진입 대비 보험 (탭 가드는 AppShellViewModel, 규칙 §8)
    case requireSignIn
    case openReputation
    case openEditProfile(nickname: String, avatarId: Int?)
    case openPaymentMethod
    case openCoinHistory
    // 코인 충전 화면 (M-12-4·M-12-6)
    case openCharge
    case openSettlementAccount
    case openEarnings
    case openNotifications
    case openDeleteAccount
    // 로그아웃 완료 → 홈 탭으로 (세션 정리는 shared가 수행)
    case signedOut
    case showNotice(message: String)
}
