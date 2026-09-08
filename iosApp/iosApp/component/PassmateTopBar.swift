import SwiftUI

// 공통 상단 앱바 — 시안 `header` 프레임 기준. 확인한 화면 전부가 같은 규격이다:
// M-12 마이(349:9684) · M-09 명성(349:9771) · M-T4 정산(349:10200) · M-12-1 계정 정보(437:5425) · M-14 방 리포트(432:5367).
// Compose PassmateTopBar.kt와 1:1. 구성은 [뒤로가기 24] gap12 [타이틀] spacer [우측 액션], 좌우 20 · 하단 16.
//
// 여백은 컴포넌트가 고정한다 — 화면마다 달라지면 시안과 어긋나기 때문이다.
// 앱바는 ScrollView **밖**에 두고, 좌우 여백은 본문이 따로 준다
// (`VStack { PassmateTopBar(...); ScrollView { ... }.padding(.horizontal, 20) }`).
enum PassmateTopBarStyle {
    // 탭 루트·명성·정산 — 시안 24pt (헤더 타이틀 높이 29 = 24 x 1.2)
    case root

    // 그 밖의 상세 화면 — 시안 heading-md 20pt (높이 24 = 20 x 1.2)
    case detail

    var fontSize: CGFloat {
        switch self {
        case .root: return 24
        case .detail: return 20
        }
    }

    var kerning: CGFloat {
        switch self {
        case .root: return -0.48
        case .detail: return -0.4
        }
    }
}

private enum Spec {
    static let horizontalPadding: CGFloat = 20

    // 시안은 타이틀이 화면 최상단에서 y=56이다. 상태바(iPhone 47)는 세이프에어리어가 이미 먹으므로
    // 그 아래 여백만 남긴다 — 56 - 47 = 9에 가까운 12로 전 화면을 통일한다
    static let topPadding: CGFloat = 12

    static let bottomPadding: CGFloat = 16

    static let contentGap: CGFloat = 12
}

struct PassmateTopBar<Trailing: View>: View {
    let title: String

    var onBack: (() -> Void)?

    var style: PassmateTopBarStyle = .detail

    @ViewBuilder let trailing: () -> Trailing

    var body: some View {
        HStack(spacing: Spec.contentGap) {
            if let onBack {
                PassmateBackButton(onClick: onBack)
            }
            Text(title)
                .font(.system(size: style.fontSize, weight: .bold))
                .kerning(style.kerning)
                .foregroundColor(PassmateColors.textPrimary)
            Spacer()
            trailing()
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, Spec.horizontalPadding)
        .padding(.top, Spec.topPadding)
        .padding(.bottom, Spec.bottomPadding)
    }
}

extension PassmateTopBar where Trailing == EmptyView {
    init(
        title: String,
        onBack: (() -> Void)? = nil,
        style: PassmateTopBarStyle = .detail
    ) {
        self.init(title: title, onBack: onBack, style: style) {
            EmptyView()
        }
    }
}
