import SwiftUI
import UIKit

// 시스템 내비게이션 바 숨김 — 화면은 전부 자체 헤더(PassmateTopBar)를 쓰므로 루트·push 어느 레벨에서도 시스템 바를 그리지 않는다.
// 내비바 숨김은 이 modifier 하나로만 한다(`.navigationBarHidden(true)`를 화면에서 직접 쓰지 않는다).
// 붙이는 곳 3종: NavigationView 루트 콘텐츠(ContentView의 VStack) · 각 탭 콘텐츠(4곳) · RouteStackLevel(push 레벨마다).
//
// iOS 15는 NavigationView 루트(탭 셸)의 **첫 표시**에서 `.navigationBarHidden(true)`(SwiftUI preference)가 UINavigationController에
// 반영되지 않는다 — 앱 첫 진입 시 바 높이(44)만큼 빈 띠가 생기고(scroll edge 외관이라 투명, 스크롤하면 바가 드러난다),
// push 후 pop으로 루트가 다시 나타나 viewWillAppear를 타야 사라진다. push 화면은 push 전에 렌더돼 처음부터 정상이다.
// 루트 안의 TabView는 UITabBarController라 탭마다 호스팅 컨트롤러가 하나씩 더 있고 저마다 내비 preference를 브리지하므로,
// preference는 루트뿐 아니라 각 탭 콘텐츠에도 건다(탭 호스트가 "숨김 없음"으로 브리지해 루트의 값을 덮지 않게).
//
// 첫 표시는 UIKit이 직접 숨긴다. 2026-09-14의 첫 시도(빈 컨트롤러를 심어 willMove(toParent:)·viewWillAppear에서 한 번 숨김)는
// iOS 15 실기기에서 듣지 않았다 — SwiftUI가 브리지 컨트롤러를 어느 부모에 언제 붙이는지에 기댄 한 번짜리 시점이라,
// 그 시점에 조상을 못 찾거나 SwiftUI가 그 뒤에 바를 되살리면 손쓸 데가 없었다. 그래서 뷰 뒤에 빈 UIView를 심고
//   1. 조상 UINavigationController를 컨트롤러 부모 체인이 아니라 **응답자 체인**(view → superview → 호스팅 컨트롤러 → …)으로 찾는다
//   2. 창에 붙는 순간(didMoveToWindow, 첫 프레임 전)과 **레이아웃마다**(layoutSubviews), 그리고 한 런루프 뒤에 거듭 숨긴다
// SwiftUI가 뒤늦게 바를 되살리면 본문이 44 줄어 이 뷰의 레이아웃이 다시 돌고, 그 레이아웃에서 다시 숨긴다 — 되살아난 채로 남을 수 없다.
// pop으로 루트가 다시 창에 붙을 때도 didMoveToWindow가 돌아 재확인한다. (규칙 §2-1: iOS 버전 분기는 공통 컴포넌트 안에서만)
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

// iOS 15: 뷰 뒤에 빈 UIView를 심고 조상 UINavigationController의 바를 숨긴다
private struct NativeNavigationBarHider: UIViewRepresentable {
    func makeUIView(context: Context) -> NavigationBarHidingView {
        NavigationBarHidingView()
    }

    func updateUIView(_ uiView: NavigationBarHidingView, context: Context) {
        uiView.hideNavigationBar()
    }
}

private final class NavigationBarHidingView: UIView {
    // 응답자 체인을 거슬러 첫 UINavigationController — 이 뷰가 화면에 있는 한 셸의 NavigationView가 만든 컨트롤러에 닿는다
    // (탭 콘텐츠에서는 UITabBarController를 지나 위로 올라간다). SwiftUI의 컨트롤러 부모 배선 시점에 기대지 않는다
    private var ancestorNavigationController: UINavigationController? {
        var responder = next

        while let current = responder {
            if let navigationController = current as? UINavigationController {
                return navigationController
            } else {
                responder = current.next
            }
        }
        return nil
    }

    // animated: false — 레이아웃 도중에 불리므로 애니메이션을 주면 바가 걷히는 모습이 프레임에 남는다
    func hideNavigationBar() {
        let navigationController = ancestorNavigationController

        if let navigationController, !navigationController.isNavigationBarHidden {
            navigationController.setNavigationBarHidden(true, animated: false)
        }
    }

    // 창에 붙는 순간(첫 표시·pop 복귀) — 첫 프레임 전이다. 같은 런루프에서 SwiftUI가 바를 되살릴 여지가 있어 한 번 더 예약한다
    override func didMoveToWindow() {
        super.didMoveToWindow()
        if window != nil {
            hideNavigationBar()
            DispatchQueue.main.async { [weak self] in
                self?.hideNavigationBar()
            }
        }
    }

    // 바가 나타나면 본문 높이가 바뀌어 이 뷰의 레이아웃이 다시 돈다 — 그때 되돌린다
    override func layoutSubviews() {
        super.layoutSubviews()
        hideNavigationBar()
    }

    override init(frame: CGRect) {
        super.init(frame: frame)
        isUserInteractionEnabled = false
        backgroundColor = .clear
    }

    required init?(coder: NSCoder) {
        fatalError("NavigationBarHidingView는 코드로만 생성한다")
    }
}
