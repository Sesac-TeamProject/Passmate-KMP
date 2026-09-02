import SwiftUI

// 공통 뒤로가기 버튼 — 상세 화면 헤더 좌측 (Compose PassmateBackButton.kt와 1:1)
// 레이아웃 점유는 24pt 정사각으로 고정하고, 터치 영역만 사방 10pt 넓힌다.
struct PassmateBackButton: View {
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            Text("←")
                .font(.system(size: 22))
                .foregroundColor(PassmateColors.textPrimary)
                .frame(width: 24, height: 24)
                .contentShape(Rectangle().inset(by: -10))
        }
        .buttonStyle(.plain)
        .accessibilityLabel("뒤로 가기")
    }
}
