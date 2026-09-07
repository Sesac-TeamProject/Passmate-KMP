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
            if let result = uiState.result {
                sessionInfoCard(result, host: uiState.host)
            }
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
            FlowLayout(RatingTag.companion.all, id: \.self, spacing: 8) { tag in
                tagChip(tag)
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
        // 시안은 시트 상단에서 제목까지 62pt를 둔다(손잡이 20 + 여백). 시스템 손잡이 높이를 뺀 값
        .padding(.top, 48)
        .padding(.bottom, 28)
    }

    // 시안(M-06 v2)의 세션 정보 카드 — 선생님 아바타·이름·등급 배지 + 방 제목·문항 수·내 제출.
    // 선생님 정보는 결과 응답에 없어 별도 조회한다(계약 갭 G-8) — 아직 안 왔으면 방 정보 줄만 그린다
    private func sessionInfoCard(_ result: SessionResult, host: HostProfile?) -> some View {
        HStack(spacing: 10) {
            if let host {
                StudentAvatarView(avatarId: host.avatarId.map { Int(truncating: $0) } ?? 0)
                    .frame(width: 36, height: 36)
            }
            VStack(alignment: .leading, spacing: 2) {
                if let host {
                    HStack(spacing: 6) {
                        Text("\(host.nickname) 선생님")
                            .font(.system(size: 14, weight: .medium))
                            .kerning(-0.28)
                            .foregroundColor(PassmateColors.textPrimary)
                        if let level = localLevel(host) {
                            ReputationBadgeView(level: level)
                        }
                    }
                }
                Text("\(result.roomTitle) · \(result.questionCount)문항 · 내 제출 \(result.submitCount)/\(result.questionCount)")
                    .font(.system(size: 12))
                    .kerning(-0.24)
                    .foregroundColor(PassmateColors.textSecondary)
            }
            Spacer(minLength: 0)
        }
        .padding(.leading, 12)
        .padding(.trailing, 14)
        .padding(.vertical, 10)
        .background(PassmateColors.fieldGray)
        .cornerRadius(14)
    }

    // Kotlin(Shared)의 HostLevel과 화면용 Swift enum HostLevel은 이름이 같아 서로 가린다 — 여기서 옮겨 담는다
    private func localLevel(_ host: HostProfile) -> HostLevel? {
        if let level = host.level {
            return HostLevel.from(Int(level.level))
        } else {
            return nil
        }
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

// MARK: - 프리뷰 (Figma 시안 비교용, 백엔드 불필요)

#Preview("미평가") {
    RatingSectionView(
        uiState: ResultUiState(),
        onAction: { _ in }
    )
}

#Preview("평가 완료 (hasRated)") {
    RatingSectionView(
        uiState: ResultUiState(
            ratingStars: 5,
            ratingTags: [.clearExplanation, .helpfulHints],
            ratingComment: "설명이 명확하고 힌트가 큰 도움이 됐어요!",
            hasRated: true
        ),
        onAction: { _ in }
    )
}
