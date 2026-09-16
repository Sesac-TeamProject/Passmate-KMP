import CoreGraphics

// 셸이 고를 수 있는 내비게이션 배치 — Compose navigation/AppShellLayout.kt 미러 (규칙 §14)
enum AppShellLayout {
    case bottomBar
    case rail
}

// 가로폭 하나로 배치를 정한다. 판정은 여기 한 곳에만 둔다
enum AppShellLayoutPolicy {
    // Material3 window size class의 compact/medium 경계
    static let railMinWidth: CGFloat = 600

    // 전 화면이 모바일 폭 기준 시안이라, 넓은 창에서는 본문을 이 폭으로 묶는다
    static let contentMaxWidth: CGFloat = 600

    // horizontalSizeClass가 아니라 실제 폭으로 잰다 — size class는 iPad 분할 화면에서
    // 경계가 600pt와 어긋나 3플랫폼 판정이 달라진다 (스펙 2026-09-16 §1-2)
    static func layoutFor(width: CGFloat) -> AppShellLayout {
        return width >= railMinWidth ? .rail : .bottomBar
    }
}
