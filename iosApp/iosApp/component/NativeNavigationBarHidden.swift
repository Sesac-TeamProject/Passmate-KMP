import SwiftUI
import UIKit

// 시스템 내비게이션 바 숨김 — 화면은 전부 자체 헤더(PassmateTopBar)를 쓰므로 루트·push 어느 레벨에서도 시스템 바를 그리지 않는다.
// 내비바 숨김은 이 modifier 하나로만 한다(`.navigationBarHidden(true)`를 화면에서 직접 쓰지 않는다).
//
// SwiftUI `.navigationBarHidden(true)`는 preference다 — 호스팅 컨트롤러가 본문을 한 번 렌더해야 값이 정해지고,
// 그 뒤에 UINavigationController에 전달된다. iOS 15는 NavigationView 루트(탭 셸)의 **첫 표시**에서 이 전달이 빠진다:
// 루트 호스트의 viewWillAppear 시점엔 preference가 아직 없어 바를 보이는 채로 레이아웃하고, 렌더 뒤의 갱신은 반영되지 않는다.
// 시스템 바는 iOS 15 기본 외관(scroll edge)에서 투명이라 바 높이 44만큼 콘텐츠가 밀린 빈 띠로 보이고,
// push 후 pop으로 루트가 다시 나타나 viewWillAppear가 확정된 preference를 읽은 뒤에야 띠가 사라진다.
// push 화면은 SwiftUI가 push 전에 렌더해 두므로 처음부터 숨겨진다. iOS 16+는 루트도 첫 표시부터 반영된다.
//
// 그래서 iOS 15는 UIKit으로 조상 UINavigationController의 바를 직접 숨긴다 — 뷰 뒤에 빈 컨트롤러를 심어 계층에 붙는 즉시
// (첫 프레임 전) 숨기고, 다시 나타날 때마다 재확인한다. NativeTabBarHidden과 같은 기법이다
// (규칙 §2-1: iOS 버전 분기는 공통 컴포넌트 안에서만).
extension View {
    func passmateHidesNativeNavigationBar() -> some View {
        modifier(NativeNavigationBarHiddenModifier())
    }
}

private struct NativeNavigationBarHiddenModifier: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 16.0, *) {
            content.navigationBarHidden(true)
        } else {
            content
                .navigationBarHidden(true)
                .background(NativeNavigationBarHider())
        }
    }
}

// iOS 15: 뷰 뒤에 빈 컨트롤러를 심고 조상 UINavigationController의 바를 숨긴다
private struct NativeNavigationBarHider: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> NavigationBarHidingController {
        NavigationBarHidingController()
    }

    func updateUIViewController(_ uiViewController: NavigationBarHidingController, context: Context) {
        uiViewController.hideNavigationBar()
    }
}

private final class NavigationBarHidingController: UIViewController {
    // animated: false — 첫 레이아웃 도중에 불리므로 애니메이션을 주면 바가 걷히는 모습이 첫 프레임에 남는다
    private func hide(_ navigationController: UINavigationController?) {
        if let navigationController, !navigationController.isNavigationBarHidden {
            navigationController.setNavigationBarHidden(true, animated: false)
        }
    }

    func hideNavigationBar() {
        hide(navigationController)
    }

    // 부모 호스트에 붙는 순간 — 루트 호스트가 첫 레이아웃을 하는 도중이라 첫 프레임 전에 숨는다.
    // 이때 self.navigationController는 아직 nil이므로 붙을 부모를 통해 찾는다
    override func willMove(toParent parent: UIViewController?) {
        super.willMove(toParent: parent)
        hide(parent?.navigationController)
    }

    // pop으로 루트가 다시 나타날 때 SwiftUI가 바를 되돌렸다면 여기서 재확인한다
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        hideNavigationBar()
    }
}
