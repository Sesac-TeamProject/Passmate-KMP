import SwiftUI
import UIKit

// 시스템 TabView 탭 바 숨김 — 시안 v6는 커스텀 하단 바(PassmateBottomTabBar)를 쓴다.
// iOS 16+는 toolbar(.hidden, for: .tabBar), iOS 15는 UIKit으로 조상 UITabBarController의 탭 바를 숨긴다
// (규칙 §2-1: iOS 16+ API는 공통 컴포넌트 안 #available 분기로만).
//
// ⚠️ 반드시 TabView가 아니라 **각 탭의 콘텐츠**에 붙인다. TabView 자체에 붙이면 아무 일도 일어나지 않는다.
// 셸이 ZStack이던 시절에는 커스텀 바가 위에 겹쳐 그려져 숨김이 실패해도 가려졌지만,
// 탭바가 자리를 차지하도록 VStack으로 바꾼 뒤로는 가려 줄 것이 없다 — 네이티브 바가 그대로 드러난다.
extension View {
    func passmateHidesNativeTabBar() -> some View {
        modifier(NativeTabBarHiddenModifier())
    }
}

private struct NativeTabBarHiddenModifier: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 16.0, *) {
            content.toolbar(.hidden, for: .tabBar)
        } else {
            content.background(NativeTabBarHider())
        }
    }
}

// iOS 15: 뷰 뒤에 빈 컨트롤러를 심고 조상 UITabBarController를 찾아 탭 바를 숨긴다
private struct NativeTabBarHider: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> TabBarHidingController {
        TabBarHidingController()
    }

    func updateUIViewController(_ uiViewController: TabBarHidingController, context: Context) {
        uiViewController.hideTabBar()
    }
}

private final class TabBarHidingController: UIViewController {
    func hideTabBar() {
        tabBarController?.tabBar.isHidden = true
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        hideTabBar()
    }
}
