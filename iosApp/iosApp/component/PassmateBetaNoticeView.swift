import SwiftUI

// 베타 안내 배너 — 시안 "13 · 베타 운영"의 banner/베타 안내(1111:9662). BETA 칩 + 제목 한 줄 + 설명.
// 잠긴 버튼 **바로 위**에 둔다: 왜 안 눌리는지를 버튼보다 먼저 읽어야 한다.
// 여러 줄 설명은 message에 개행(\n)으로 준다. Compose component/PassmateBetaNotice.kt와 1:1 미러
//
// 제목은 시안에서 고정 레이어다(바뀌는 자리는 본문뿐) — 호출부가 매번 적지 않게 컴포넌트가 기본값으로 갖는다
struct PassmateBetaNoticeView: View {
    var title: String = defaultTitle

    let message: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 8) {
                Text("BETA")
                    .font(.system(size: 10, weight: .bold))
                    .kerning(0.4)
                    .foregroundColor(PassmateColors.surface)
                    .frame(height: PassmateBetaNoticeMetrics.chipLineHeight)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 2)
                    .background(PassmateColors.primary)
                    .clipShape(Capsule())
                Text(title)
                    .font(.system(size: 13, weight: .bold))
                    .kerning(-0.26)
                    .foregroundColor(PassmateColors.textPrimary)
                    .frame(height: PassmateBetaNoticeMetrics.titleLineHeight)
            }
            Text(message)
                .font(.system(size: 12))
                .kerning(-0.24)
                .lineSpacing(PassmateBetaNoticeMetrics.bodyLineSpacing)
                .foregroundColor(PassmateColors.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.vertical, PassmateBetaNoticeMetrics.bodyVerticalInset)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(PassmateColors.backgroundMint)
        .cornerRadius(12)
    }
}

// 줄 높이 — SwiftUI에는 lineHeight가 없어(iOS 15 기준) Compose의 lineHeight를 환산해 둔다.
// 이게 없으면 SF 기본 행높이로 그려져 배너가 시안(1111:9662, 64pt)보다 6pt 낮아진다 (실측 58.3 → 64.0)
private enum PassmateBetaNoticeMetrics {
    // Compose lineHeight 14 — 한 줄이라 lineSpacing이 먹지 않는다. 글자 상자 높이를 고정한다 (칩 = 14 + 위아래 2 = 18)
    static let chipLineHeight: CGFloat = 14

    // Compose lineHeight 18 — 위와 같은 이유로 고정
    static let titleLineHeight: CGFloat = 18

    // Compose lineHeight 18 - SF 12pt 기본 행높이 14.33
    static let bodyLineSpacing: CGFloat = 3.67

    // lineSpacing은 줄 '사이'에만 들어간다 — 첫 줄 위·끝 줄 아래의 반쪽 행간을 채워 n줄 = n x 18로 맞춘다
    static let bodyVerticalInset: CGFloat = bodyLineSpacing / 2
}

// Compose PassmateBetaNotice.kt의 DEFAULT_TITLE과 1:1
private let defaultTitle = "현재는 베타 버전입니다."
