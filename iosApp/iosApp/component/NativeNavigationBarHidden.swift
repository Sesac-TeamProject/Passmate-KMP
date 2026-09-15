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
// preference는 루트뿐 아니라 각 탭 콘텐츠에도 건다.
//
// iOS 15는 그 위에 UIKit이 직접 숨긴다. 실기기 이력:
//   - 09-14: 빈 컨트롤러를 심어 willMove(toParent:)·viewWillAppear에서 한 번 숨김 → 듣지 않음(SwiftUI의 컨트롤러 부모 배선에 기댄 시점).
//   - 09-15 1차: 빈 UIView가 응답자 체인으로 컨트롤러를 찾아 창에 붙을 때 + **레이아웃마다** 숨김 → 참여한 방 탭에서 SwiftUI 쪽
//     무언가가 바를 되살리고 우리가 레이아웃마다 되감아 **타이틀이 위아래로 떨리는 무한 핑퐁**이 났다.
// 그래서 지금은 되감기를 레이아웃에 걸지 않는다(핑퐁 원천 차단). 대신 두 겹이다:
//   1. NavigationBarHidingView — 창에 붙는 순간(첫 프레임 전·pop 복귀·탭 전환)과 한 런루프 뒤, 딱 두 번만 숨긴다.
//   2. UINavigationController 확장 — 셸의 컨트롤러에 "계속 숨김" 표식을 달고, 표식이 붙은 컨트롤러에 대한
//      setNavigationBarHidden(false)를 숨김으로 바꾼다(교환 구현). 누가 되살리려 해도 바가 나타나지 않으니 되감을 일이 없고,
//      따라서 떨림도 없다. 표식이 없는 컨트롤러(공유 시트·사진 선택기 등 시스템 것)는 원래대로 동작한다.
// (규칙 §2-1: iOS 버전 분기는 공통 컴포넌트 안에서만. 최소 타깃이 16으로 오르면 이 파일의 iOS 15 분기는 통째로 지운다)
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

// iOS 15: 뷰 뒤에 빈 UIView를 심고 조상 UINavigationController에 "계속 숨김"을 건다
private struct NativeNavigationBarHider: UIViewRepresentable {
    func makeUIView(context: Context) -> NavigationBarHidingView {
        NavigationBarHidingView()
    }

    // SwiftUI 갱신마다 되감지 않는다 — 갱신 ↔ 되살림 핑퐁의 씨앗이 된다. 숨김은 창에 붙는 시점에만 건다
    func updateUIView(_ uiView: NavigationBarHidingView, context: Context) {}
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

    private func keepNavigationBarHidden() {
        let navigationController = ancestorNavigationController

        if let navigationController {
            navigationController.passmateKeepNavigationBarHidden()
        }
    }

    // 창에 붙는 순간(첫 표시·pop 복귀·탭 전환) — 첫 프레임 전이다. 같은 런루프 안에서 SwiftUI가 바를 되살릴 여지가 있어
    // 한 런루프 뒤 한 번 더 확인한다. 그 뒤로는 되감지 않는다(되살림 자체를 아래 확장이 막는다)
    override func didMoveToWindow() {
        super.didMoveToWindow()
        if window != nil {
            keepNavigationBarHidden()
            DispatchQueue.main.async { [weak self] in
                self?.keepNavigationBarHidden()
            }
        }
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

// iOS 15 전용 — "계속 숨김" 표식이 붙은 컨트롤러에서는 setNavigationBarHidden(false)가 숨김으로 바뀐다.
// 교환은 프로세스에서 한 번만 일어나고, 표식은 약한 참조 집합이라 컨트롤러가 사라지면 함께 사라진다
// (sessionGeneration으로 NavigationView가 재생성돼도 새 컨트롤러에 다시 표식만 달면 된다)
private extension UINavigationController {
    private static let keptHidden = NSHashTable<UINavigationController>.weakObjects()

    private static let installKeepHiddenOverride: Void = {
        UINavigationController.exchange(
            #selector(UINavigationController.setNavigationBarHidden(_:animated:)),
            with: #selector(UINavigationController.passmateSetNavigationBarHidden(_:animated:))
        )
        UINavigationController.exchange(
            #selector(setter: UINavigationController.isNavigationBarHidden),
            with: #selector(UINavigationController.passmateSetNavigationBarHidden(_:))
        )
    }()

    private static func exchange(_ original: Selector, with replacement: Selector) {
        let originalMethod = class_getInstanceMethod(UINavigationController.self, original)
        let replacementMethod = class_getInstanceMethod(UINavigationController.self, replacement)

        if let originalMethod, let replacementMethod {
            method_exchangeImplementations(originalMethod, replacementMethod)
        }
    }

    private var passmateKeepsNavigationBarHidden: Bool {
        UINavigationController.keptHidden.contains(self)
    }

    // 교환 뒤에는 이 이름이 UIKit 원본 구현을 가리킨다 — 안에서 같은 이름을 부르면 원본이 실행된다
    @objc private dynamic func passmateSetNavigationBarHidden(_ hidden: Bool, animated: Bool) {
        if passmateKeepsNavigationBarHidden && !hidden {
            passmateSetNavigationBarHidden(true, animated: false)
        } else {
            passmateSetNavigationBarHidden(hidden, animated: animated)
        }
    }

    @objc private dynamic func passmateSetNavigationBarHidden(_ hidden: Bool) {
        if passmateKeepsNavigationBarHidden && !hidden {
            passmateSetNavigationBarHidden(true)
        } else {
            passmateSetNavigationBarHidden(hidden)
        }
    }

    // animated: false — 레이아웃 도중에 불릴 수 있어 애니메이션을 주면 바가 걷히는 모습이 프레임에 남는다
    func passmateKeepNavigationBarHidden() {
        _ = UINavigationController.installKeepHiddenOverride

        UINavigationController.keptHidden.add(self)
        if !isNavigationBarHidden {
            setNavigationBarHidden(true, animated: false)
        }
    }
}
