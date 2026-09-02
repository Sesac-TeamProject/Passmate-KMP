import SwiftUI

// 아이콘 리소스 로더 (규칙 §11-3) — Compose PassmateIcon의 iOS 미러.
// 에셋 이름은 Compose PassmateIcons 항목의 iOS 사본 이름과 1:1이다
// (DoorOpen · Bookmark · AlertCircle). 색은 템플릿 렌더링으로 준다 (규칙 §11-2)
struct PassmateIconView: View {
    let asset: String

    let tint: Color

    let size: CGFloat

    var body: some View {
        Image(asset)
            .renderingMode(.template)
            .resizable()
            .scaledToFit()
            .foregroundColor(tint)
            .frame(width: size, height: size)
    }
}
