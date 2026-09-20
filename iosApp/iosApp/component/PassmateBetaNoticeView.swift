import SwiftUI

// 베타 안내 배너 — 시안 "13 · 베타 운영"의 banner/베타 안내(1111:9662). BETA 칩 + 제목 한 줄 + 설명.
// 잠긴 버튼 **바로 위**에 둔다: 왜 안 눌리는지를 버튼보다 먼저 읽어야 한다.
// 여러 줄 설명은 body에 개행(\n)으로 준다. Compose component/PassmateBetaNotice.kt와 1:1 미러
struct PassmateBetaNoticeView: View {
    let title: String

    let message: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 8) {
                Text("BETA")
                    .font(.system(size: 10, weight: .bold))
                    .kerning(0.4)
                    .foregroundColor(PassmateColors.surface)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 2)
                    .background(PassmateColors.primary)
                    .clipShape(Capsule())
                Text(title)
                    .font(.system(size: 13, weight: .bold))
                    .kerning(-0.26)
                    .foregroundColor(PassmateColors.textPrimary)
            }
            Text(message)
                .font(.system(size: 12))
                .kerning(-0.24)
                .lineSpacing(3)
                .foregroundColor(PassmateColors.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(PassmateColors.backgroundMint)
        .cornerRadius(12)
    }
}
