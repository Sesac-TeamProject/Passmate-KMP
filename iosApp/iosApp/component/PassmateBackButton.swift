import SwiftUI

// 공통 뒤로가기 버튼 — 상세 화면 헤더 좌측 (Compose PassmateBackButton.kt와 1:1)
// 시안 `icon/arrow-left`(223:2826) — 24pt 그리드·선 2pt·라운드 캡, ink #1B1F24.
// 레이아웃 점유는 24pt 정사각으로 고정하고, 터치 영역만 사방 10pt 넓힌다.
struct PassmateBackButton: View {
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            PassmateIconView(
                icon: .arrowLeft,
                tint: PassmateColors.textPrimary,
                size: 24
            )
            .contentShape(Rectangle().inset(by: -10))
        }
        .buttonStyle(.plain)
        .accessibilityLabel("뒤로 가기")
    }
}
