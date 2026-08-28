import SwiftUI
import Shared

// T056(US4) 서술형 AI 분석 피드백 뷰 — Figma "UI 디자인 v6" M-06(349:9433) 카드 미러.
// 분석 중/완료/실패/한도 소진(SKIPPED) 상태 표시, 정오·점수 확인은 막지 않는다 (규칙 §10)
struct FeedbackSectionView: View {
    let question: QuestionResult

    var body: some View {
        VStack(spacing: 0) {
            header
            VStack(alignment: .leading, spacing: 9) {
                feedbackBody
                hostReviewRow
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .padding(.bottom, 14)
        }
        .background(PassmateColors.surface)
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(PassmateColors.border, lineWidth: 1))
        .cornerRadius(20)
    }

    private var header: some View {
        HStack {
            Text("Q\(question.questionNo) · AI 분석 (참고 의견)")
                .font(.system(size: 14, weight: .medium))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.surface)
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 11)
        .background(PassmateColors.primary)
    }

    @ViewBuilder
    private var feedbackBody: some View {
        let feedback = question.aiFeedback

        if feedback?.status == AiFeedbackStatus.done {
            doneFeedback(feedback)
        } else if feedback?.status == AiFeedbackStatus.pending {
            statusMessage("AI가 답변을 분석하고 있어요…")
        } else if feedback?.status == AiFeedbackStatus.failed {
            statusMessage("분석을 완료하지 못했어요 — 정오·점수는 위에서 확인할 수 있어요")
        } else if feedback?.status == AiFeedbackStatus.skipped {
            statusMessage("이번 세션은 무료 분석 한도를 초과해 AI 분석이 제공되지 않았어요")
        } else {
            statusMessage("이 문항에는 AI 분석이 없어요")
        }
    }

    @ViewBuilder
    private func doneFeedback(_ feedback: AiFeedback?) -> some View {
        if let feedback {
            if !feedback.coveredConcepts.isEmpty {
                feedbackPoint(
                    dotColor: PassmateColors.primary,
                    text: "핵심 포함 — \(feedback.coveredConcepts.joined(separator: ", "))"
                )
            }
            if let shortage = shortageText(feedback) {
                feedbackPoint(dotColor: PassmateColors.weakTopicText, text: "부족 — \(shortage)")
            }
            if let improvement = feedback.improvement, !improvement.isEmpty {
                feedbackPoint(dotColor: PassmateColors.textPrimary, text: "제안 — \(improvement)")
            }
        }
    }

    private func shortageText(_ feedback: AiFeedback) -> String? {
        if !feedback.missingConcepts.isEmpty {
            return feedback.missingConcepts.joined(separator: ", ")
        } else if let weaknesses = feedback.weaknesses, !weaknesses.isEmpty {
            return weaknesses
        } else {
            return nil
        }
    }

    private func feedbackPoint(dotColor: Color, text: String) -> some View {
        HStack(spacing: 8) {
            Circle()
                .fill(dotColor)
                .frame(width: 6, height: 6)
            Text(text)
                .font(.system(size: 14, weight: .medium))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.textPrimary)
            Spacer(minLength: 0)
        }
    }

    private func statusMessage(_ message: String) -> some View {
        Text(message)
            .font(.system(size: 14, weight: .medium))
            .kerning(-0.28)
            .foregroundColor(PassmateColors.textSecondary)
    }

    // 첨삭 입력은 파트2 T072 — 여기선 도착 시 표시만, 미도착이면 안내 문구 (M-06 하단)
    @ViewBuilder
    private var hostReviewRow: some View {
        if let review = question.hostReview {
            // T072(US8): 선생님 첨삭을 AI와 구분 표시, 최종 점수는 보정 우선 (FR-034~035)
            VStack(alignment: .leading, spacing: 4) {
                Text("선생님 첨삭")
                    .font(.system(size: 12, weight: .bold))
                    .kerning(-0.24)
                    .foregroundColor(PassmateColors.primaryDeep)
                Text(review.comment)
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.textPrimary)
                if let improvement = review.improvement, !improvement.isEmpty {
                    Text("개선 — \(improvement)")
                        .font(.system(size: 13))
                        .kerning(-0.26)
                        .foregroundColor(PassmateColors.textSecondary)
                }
                if let adjustedScore = review.adjustedScore {
                    Text("최종 점수 \(Int(truncating: adjustedScore))점 · 선생님 보정 반영")
                        .font(.system(size: 13, weight: .medium))
                        .kerning(-0.26)
                        .foregroundColor(PassmateColors.primaryDeep)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(12)
            .background(PassmateColors.backgroundMint)
            .cornerRadius(12)
        } else {
            Text("선생님 코멘트가 도착하면 여기에 표시돼요")
                .font(.system(size: 12))
                .kerning(-0.24)
                .foregroundColor(PassmateColors.textSecondary)
        }
    }
}
