import Foundation

enum DeleteAccountEvent {
    // 탈퇴 완료 → 홈으로 (세션 정리는 shared가 수행)
    case deleted
    case showNotice(message: String)
}
