import Foundation

enum SettingsAction {
    case enter

    // 확인 알림을 거친 뒤 호출된다 — 알림 소유는 화면 (규칙 §11-1)
    case confirmDeleteAccount
}
