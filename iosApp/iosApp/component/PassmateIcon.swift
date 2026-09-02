import SwiftUI

// 아이콘 리소스 키 — Compose PassmateIcons enum 미러. 값은 Assets.xcassets의 에셋 이름이다 (규칙 §11-3)
enum PassmateIcons {
    static let doorOpen = "DoorOpen"

    static let alertCircle = "AlertCircle"
}

// 공통 아이콘 — Compose PassmateIcon 미러. 지오메트리는 Assets.xcassets가,
// 표시 색은 호출부가 PassmateColors 토큰으로 준다 (규칙 §11-2·§11-3)
struct PassmateIcon: View {
    let icon: String

    let tint: Color

    let size: CGFloat

    var body: some View {
        Image(icon)
            .renderingMode(.template)
            .resizable()
            .scaledToFit()
            .foregroundColor(tint)
            .frame(width: size, height: size)
    }
}
