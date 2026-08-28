import SwiftUI

// 임시 셸 화면 — 본 구현은 T050(풀이·랭킹 미러)에서 M-03 v6 디자인으로 대체된다 (Compose PlayScreen.kt 미러)
struct PlayView: View {
    let pin: String

    var body: some View {
        VStack(spacing: 0) {
            PassyMascotView()
                .frame(width: 80, height: 88)
            Text("곧 문제가 시작돼요!")
                .font(.system(size: 20, weight: .bold))
                .kerning(-0.4)
                .foregroundColor(PassmateColors.textPrimary)
                .padding(.top, 16)
            Text("풀이 화면은 준비 중이에요")
                .font(.system(size: 14))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.textSecondary)
                .padding(.top, 6)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(PassmateColors.surface.ignoresSafeArea())
    }
}
