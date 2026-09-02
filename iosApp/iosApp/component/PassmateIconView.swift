import SwiftUI

// 공통 아이콘 — Compose PassmateIcon(expect/actual)의 iOS 미러다.
// 에셋은 벡터를 보존한 템플릿이라 색은 리소스가 아니라 호출부 토큰으로 준다 (규칙 §11-2)
struct PassmateIconView: View {
    let icon: PassmateIcons

    let tint: Color

    let size: CGFloat

    var body: some View {
        Image(icon.rawValue)
            .renderingMode(.template)
            .resizable()
            .scaledToFit()
            .foregroundColor(tint)
            .frame(width: size, height: size)
    }
}
