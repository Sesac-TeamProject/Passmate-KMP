import SwiftUI
import UIKit

// 시트 높이(반높이·전체) 공통 modifier — iOS 16+는 presentationDetents, iOS 15는 UIKit UISheetPresentationController로 동일 결과
// (스펙 2026-08-31 §4, 규칙 §2-1: iOS 16+ API는 공통 컴포넌트 안 #available 분기로만)
enum PassmateSheetDetent {
    case medium
    case large
}

extension View {
    func passmateDetents(_ detents: [PassmateSheetDetent]) -> some View {
        modifier(PassmateSheetDetentsModifier(detents: detents))
    }
}

private struct PassmateSheetDetentsModifier: ViewModifier {
    let detents: [PassmateSheetDetent]

    @available(iOS 16.0, *)
    private var nativeDetents: Set<PresentationDetent> {
        Set(detents.map { detent -> PresentationDetent in
            switch detent {
            case .medium: return .medium
            case .large: return .large
            }
        })
    }

    func body(content: Content) -> some View {
        if #available(iOS 16.0, *) {
            content.presentationDetents(nativeDetents)
        } else {
            content.background(SheetDetentsBridge(detents: detents))
        }
    }
}

// iOS 15: 시트 콘텐츠 뒤에 빈 컨트롤러를 심고, 조상 UISheetPresentationController의 detents를 설정한다.
// 선택 detent를 따로 지정하지 않으면 배열의 첫 항목으로 열린다([.medium, .large] → 반높이 시작)
private struct SheetDetentsBridge: UIViewControllerRepresentable {
    let detents: [PassmateSheetDetent]

    func makeUIViewController(context: Context) -> SheetDetentsController {
        SheetDetentsController(detents: detents.map(\.uiKitDetent))
    }

    func updateUIViewController(_ uiViewController: SheetDetentsController, context: Context) {
        uiViewController.detents = detents.map(\.uiKitDetent)
        uiViewController.applyDetents()
    }
}

private final class SheetDetentsController: UIViewController {
    var detents: [UISheetPresentationController.Detent]

    func applyDetents() {
        sheetPresentationController?.detents = detents
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        applyDetents()
    }

    init(detents: [UISheetPresentationController.Detent]) {
        self.detents = detents
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) {
        fatalError("SheetDetentsController는 코드로만 생성한다")
    }
}

private extension PassmateSheetDetent {
    var uiKitDetent: UISheetPresentationController.Detent {
        switch self {
        case .medium: return .medium()
        case .large: return .large()
        }
    }
}
