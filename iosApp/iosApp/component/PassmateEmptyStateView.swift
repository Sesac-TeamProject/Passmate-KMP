import SwiftUI

// 빈 상태 블록 치수·타이포 — Compose PassmateEmptyState.kt의 PassmateEmptyStateSpec과 1:1
// (SwiftUI에는 lineHeight가 없어 파생값 guideLineSpacing 1개가 더 있다).
// 값은 0e2108c(참여한 방 빈 상태)가 시안 대조한 것을 그대로 옮긴 것이다.
// TODO 제목 자간 -0.19는 폰트 크기의 -1%로 리포 관례(-2%)와 어긋난다 — Compose 쪽 TODO 참조
private enum PassmateEmptyStateSpec {
    static let sectionPaddingVertical: CGFloat = 40

    static let iconCircleSize: CGFloat = 64

    static let iconSize: CGFloat = 28

    static let titleTopPadding: CGFloat = 16

    static let titleFontSize: CGFloat = 19

    static let titleKerning: CGFloat = -0.19

    static let guideTopPadding: CGFloat = 8

    static let guideFontSize: CGFloat = 14

    // Compose lineHeight 23.1(14 x 1.65) - SF 14pt 기본 행높이
    static let guideLineSpacing: CGFloat = 6.4

    static let ctaTopPadding: CGFloat = 24

    static let ctaWidth: CGFloat = 200

    static let ctaHeight: CGFloat = 52

    static let ctaCornerRadius: CGFloat = 14

    static let ctaFontSize: CGFloat = 16
}

// 목록 빈 상태 블록 (v6) — 아이콘 원형 · 제목 · 안내 문구 · CTA.
// 참여한 방(M-08)·정산 빈 상태 2종(M-T4)이 같은 시안을 쓴다. 문구와 아이콘만 화면이 정하고
// 치수·타이포는 여기서 고정한다 (규칙 §11 공통 컴포넌트 승격).
// Compose component/PassmateEmptyState.kt 미러
struct PassmateEmptyStateView: View {
    let icon: PassmateIcons

    let iconTint: Color

    let title: String

    let guide: String

    let ctaLabel: String

    let onClickCta: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            ZStack {
                Circle()
                    .fill(PassmateColors.emptyIconBg)
                PassmateIconView(
                    icon: icon,
                    tint: iconTint,
                    size: PassmateEmptyStateSpec.iconSize
                )
            }
            .frame(
                width: PassmateEmptyStateSpec.iconCircleSize,
                height: PassmateEmptyStateSpec.iconCircleSize
            )
            Text(title)
                .font(.system(size: PassmateEmptyStateSpec.titleFontSize, weight: .bold))
                .kerning(PassmateEmptyStateSpec.titleKerning)
                .foregroundColor(PassmateColors.textPrimary)
                .multilineTextAlignment(.center)
                .padding(.top, PassmateEmptyStateSpec.titleTopPadding)
            Text(guide)
                .font(.system(size: PassmateEmptyStateSpec.guideFontSize))
                .lineSpacing(PassmateEmptyStateSpec.guideLineSpacing)
                .foregroundColor(PassmateColors.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.top, PassmateEmptyStateSpec.guideTopPadding)
            Button(action: onClickCta) {
                Text(ctaLabel)
                    .font(.system(size: PassmateEmptyStateSpec.ctaFontSize, weight: .bold))
                    .foregroundColor(PassmateColors.surface)
                    .frame(
                        width: PassmateEmptyStateSpec.ctaWidth,
                        height: PassmateEmptyStateSpec.ctaHeight
                    )
                    .background(PassmateColors.primary)
                    .cornerRadius(PassmateEmptyStateSpec.ctaCornerRadius)
            }
            .padding(.top, PassmateEmptyStateSpec.ctaTopPadding)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, PassmateEmptyStateSpec.sectionPaddingVertical)
    }
}
