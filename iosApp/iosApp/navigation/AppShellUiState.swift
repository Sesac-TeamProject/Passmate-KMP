import Foundation

// 셸 상태 — 마지막 탭 선택 시점의 로그인 여부(탭 탭마다 동기 재조회, 규칙 §8)와
// 로그인 성공 후 복귀할 목적지(규칙 §7 pendingRoute, 스펙 §2-2)
struct AppShellUiState {
    var isSignedIn: Bool = false

    var pendingRoute: Route?
}
