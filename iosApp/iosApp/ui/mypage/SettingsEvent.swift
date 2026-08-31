import Foundation

enum SettingsEvent {
    // 설정은 회원 전용 — 게스트 진입 시 로그인 유도 (규칙 §8)
    case requireSignIn

    // 탈퇴 완료 → 홈으로 (세션 정리는 shared가 수행)
    case accountDeleted

    case showNotice(message: String)
}
