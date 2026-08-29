import SwiftUI
import Shared

// T080(US11) 세션 평가 시트 — Figma "UI 디자인 v6" M-06 v2(349:9492) 미러.
// 별점(1~5)+태그 다중+한 줄 후기, 제출 후 수정 불가·스킵 무불이익 (FR-042~043)
struct RatingSectionView: View {
    let uiState: ResultUiState

    let onAction: (ResultAction) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("이번 세션 어땠나요?")
                .font(.system(size: 20, weight: .bold))
                .kerning(-0.4)
                .foregroundColor(PassmateColors.textPrimary)
            Text("문제를 제출한 학생만 평가할 수 있어요 · 세션 종료 후 24시간 · 1회")
                .font(.system(size: 12))
                .kerning(-0.24)
                .foregroundColor(PassmateColors.textSecondary)
            VStack(spacing: 8) {
                StarRatingView(
                    stars: uiState.ratingStars,
                    onSelect: { onAction(.selectRatingStars(stars: $0)) }
                )
                Text(starLabel(uiState.ratingStars))
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.primaryDeep)
            }
            .frame(maxWidth: .infinity)
            FlowLayout(spacing: 8) {
                ForEach(RatingTag.companion.all, id: \.self) { tag in
                    tagChip(tag)
                }
            }
            commentField
            submitButton
            Button {
                onAction(.skipRating)
            } label: {
                Text("건너뛰기")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.textTertiary)
            }
            .frame(maxWidth: .infinity)
        }
        .padding(.horizontal, 20)
        .padding(.top, 10)
        .padding(.bottom, 28)
    }

    private func tagChip(_ tag: RatingTag) -> some View {
        let isSelected = uiState.ratingTags.contains(tag)

        return Button {
            onAction(.toggleRatingTag(tag: tag))
        } label: {
            Text(tag.label)
                .font(.system(size: 14, weight: .medium))
                .kerning(-0.28)
                .foregroundColor(isSelected ? PassmateColors.ratingTagSelectedText : PassmateColors.textSecondary)
                .padding(.horizontal, 12)
                .padding(.vertical, 7)
                .background(isSelected ? PassmateColors.ratingTagSelectedBg : PassmateColors.surface)
                .overlay(
                    Capsule().stroke(isSelected ? PassmateColors.primary : PassmateColors.border, lineWidth: 1)
                )
                .clipShape(Capsule())
        }
    }

    private var commentField: some View {
        ZStack(alignment: .topLeading) {
            TextEditor(
                text: Binding(
                    get: { uiState.ratingComment },
                    set: { onAction(.changeRatingComment(comment: $0)) }
                )
            )
            .font(.system(size: 14))
            .frame(height: 72)
            .padding(6)
            if uiState.ratingComment.isEmpty {
                Text("한 줄 후기 (선택) — 선생님에게만 보여요")
                    .font(.system(size: 14))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.textTertiary)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 14)
            }
        }
        .background(PassmateColors.fieldGray)
        .cornerRadius(14)
    }

    private var submitButton: some View {
        let enabled = uiState.ratingStars > 0

        return Button {
            onAction(.submitRating)
        } label: {
            Group {
                if uiState.isSubmittingRating {
                    ProgressView().tint(PassmateColors.surface)
                } else {
                    Text("평가 보내기")
                        .font(.system(size: 16, weight: .medium))
                        .kerning(-0.32)
                        .foregroundColor(PassmateColors.surface)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(enabled ? PassmateColors.primary : PassmateColors.textTertiary)
            .cornerRadius(16)
        }
        .disabled(!enabled || uiState.isSubmittingRating)
    }

    private func starLabel(_ stars: Int) -> String {
        switch stars {
        case 1: return "1점 · 별로예요"
        case 2: return "2점 · 아쉬워요"
        case 3: return "3점 · 괜찮아요"
        case 4: return "4점 · 좋았어요"
        case 5: return "5점 · 최고예요"
        default: return "별점을 선택해 주세요"
        }
    }
}

// 태그 칩 가로 흐름 배치 (iOS 16+) — RatingSection 전용 로컬 FlowLayout
private struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout Void) -> CGSize {
        let maxWidth = proposal.width ?? .infinity
        var rowWidth: CGFloat = 0
        var rowHeight: CGFloat = 0
        var totalHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)

            if rowWidth + size.width > maxWidth, rowWidth > 0 {
                totalHeight += rowHeight + spacing
                rowWidth = 0
                rowHeight = 0
            }
            rowWidth += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
        totalHeight += rowHeight

        return CGSize(width: maxWidth, height: totalHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout Void) {
        var x = bounds.minX
        var y = bounds.minY
        var rowHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)

            if x + size.width > bounds.maxX, x > bounds.minX {
                x = bounds.minX
                y += rowHeight + spacing
                rowHeight = 0
            }
            subview.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
    }
}
