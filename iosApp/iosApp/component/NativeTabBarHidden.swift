import SwiftUI
import UIKit

// 시스템 TabView 탭 바 숨김 — 시안 v6는 커스텀 하단 바(PassmateBottomTabBar)를 쓴다.
// iOS 16+는 toolbar(.hidden, for: .tabBar), iOS 15는 UIKit으로 조상 UITabBarController의 탭 바를 숨긴다
// (규칙 §2-1: iOS 16+ API는 공통 컴포넌트 안 #available 분기로만).
//
// 숨김이 실패해도 화면이 깨지지 않는다 — 커스텀 바가 같은 자리에 더 높게 겹쳐 그려져 가린다.
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
