import SwiftUI

// v6 공통 카드 — 흰 배경 + 보더 + 라운드 24 (Compose PassmateCard.kt와 1:1)
struct PassmateCard<Content: View>: View {
    @ViewBuilder let content: Content

    var body: some View {
        VStack(spacing: 0) {
            content
        }
        .frame(maxWidth: .infinity)
        .background(PassmateColors.surface)
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(PassmateColors.border, lineWidth: 1)
        )
        .cornerRadius(24)
    }
}
