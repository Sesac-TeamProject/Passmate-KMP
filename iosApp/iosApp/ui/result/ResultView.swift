import SwiftUI
import Shared

// Figma "UI 디자인 v6" M-06(349:9395) 미러 — 정답 링·보완 주제·문항 리스트·AI 분석 카드 + 내보내기 (T062·T056)
struct ResultView: View {
    let roomId: Int64

    var onClickHome: () -> Void = {}

    var onNavigateToSignup: () -> Void = {}

    @StateObject private var viewModel = ResultViewModel(
        getSessionResultUseCase: KoinHelper.shared.getSessionResultUseCase(),
        getLearningReportUseCase: KoinHelper.shared.getLearningReportUseCase(),
        buildReportSummaryUseCase: KoinHelper.shared.buildReportSummaryUseCase(),
        getMyParticipationUseCase: KoinHelper.shared.getMyParticipationUseCase(),
        requestGuestClaimUseCase: KoinHelper.shared.requestGuestClaimUseCase(),
        eventWatcher: KoinHelper.shared.sessionEventStreamWatcher()
    )

    @State private var shareText: String?

    var body: some View {
        ResultContentView(
            uiState: viewModel.uiState,
            onAction: { viewModel.action($0) }
        )
        .onAppear {
            viewModel.action(.enter(roomId: roomId))
        }
        .onDisappear {
            viewModel.stopWatching()
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case let .shareReport(summary):
                shareText = summary
            case .navigateToSignup:
                onNavigateToSignup()
            case .showNotice:
                break
            }
        }
        .sheet(isPresented: Binding(get: { shareText != nil }, set: { if !$0 { shareText = nil } })) {
            if let shareText {
                ShareSheet(items: [shareText])
            }
        }
    }
}

private struct ResultContentView: View {
    let uiState: ResultUiState

    let onAction: (ResultAction) -> Void

    var body: some View {
        Group {
            if uiState.isLoading {
                ProgressView()
                    .tint(PassmateColors.primary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if uiState.loadFailed || uiState.result == nil {
                errorView
            } else if let result = uiState.result {
                loadedView(result: result)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(PassmateColors.surface.ignoresSafeArea())
    }

    private var errorView: some View {
        VStack(spacing: 12) {
            Text("리포트를 불러오지 못했어요")
                .font(.system(size: 16, weight: .medium))
                .kerning(-0.32)
                .foregroundColor(PassmateColors.textPrimary)
            Button {
                onAction(.retry)
            } label: {
                Text("다시 시도")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.primaryDeep)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func loadedView(result: SessionResult) -> some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: 14) {
                    ReportHeaderCard(result: result)
                    WeakTopicsRow(topics: uiState.report?.weakTopics ?? [])
                    QuestionList(
                        questions: result.questions,
                        selectedQuestionNo: uiState.selectedQuestionNo,
                        onSelect: { onAction(.selectQuestion(questionNo: $0)) }
                    )
                    if let selected = result.questions.first(where: { Int($0.questionNo) == uiState.selectedQuestionNo }),
                       selected.aiFeedback != nil || selected.hostReview != nil {
                        FeedbackSectionView(question: selected)
                    }
                    if result.isGuest {
                        SignupPromptSectionView(onClickSignup: { onAction(.clickSignup) })
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 32)
                .padding(.bottom, 16)
            }
            exportButton
        }
    }

    private var exportButton: some View {
        Button {
            onAction(.clickExport)
        } label: {
            Text("리포트 내보내기")
                .font(.system(size: 14, weight: .medium))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.primaryDeep)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(PassmateColors.surface)
                .overlay(RoundedRectangle(cornerRadius: 16).stroke(PassmateColors.border, lineWidth: 1))
                .cornerRadius(16)
        }
        .padding(.horizontal, 20)
        .padding(.bottom, 24)
    }
}

private struct ReportHeaderCard: View {
    let result: SessionResult

    var body: some View {
        PassmateCard {
            HStack(spacing: 16) {
                correctRing
                VStack(alignment: .leading, spacing: 3) {
                    Text("내 리포트")
                        .font(.system(size: 24, weight: .bold))
                        .kerning(-0.48)
                        .foregroundColor(PassmateColors.textPrimary)
                    Text(subtitle)
                        .font(.system(size: 14, weight: .medium))
                        .kerning(-0.28)
                        .foregroundColor(PassmateColors.textSecondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                PassyMascotView()
                    .frame(width: 52, height: 57)
            }
            .padding(20)
        }
    }

    private var correctRing: some View {
        ZStack {
            Circle()
                .stroke(PassmateColors.primary, lineWidth: 6)
            VStack(spacing: 0) {
                Text("\(result.correctCount)/\(result.questionCount)")
                    .font(.system(size: 20, weight: .bold))
                    .kerning(-0.4)
                    .foregroundColor(PassmateColors.primaryDeep)
                Text("정답")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.textSecondary)
            }
        }
        .frame(width: 76, height: 76)
    }

    private var subtitle: String {
        let rankPart = result.rank.map { "\($0)위 · " } ?? ""

        return "\(result.roomTitle) · \(rankPart)\(Int(result.totalScore))점"
    }
}

private struct WeakTopicsRow: View {
    let topics: [String]

    var body: some View {
        if !topics.isEmpty {
            FlowLayout(spacing: 8) {
                Text("보완할 주제")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.textSecondary)
                    .padding(.vertical, 6)
                ForEach(topics, id: \.self) { topic in
                    Text(topic)
                        .font(.system(size: 14, weight: .medium))
                        .kerning(-0.28)
                        .foregroundColor(PassmateColors.weakTopicText)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(PassmateColors.weakTopicBg)
                        .clipShape(Capsule())
                }
            }
        }
    }
}

private struct QuestionList: View {
    let questions: [QuestionResult]

    let selectedQuestionNo: Int?

    let onSelect: (Int) -> Void

    var body: some View {
        VStack(spacing: 8) {
            ForEach(questions, id: \.questionId) { question in
                QuestionRow(
                    question: question,
                    isSelected: Int(question.questionNo) == selectedQuestionNo,
                    onTap: { onSelect(Int(question.questionNo)) }
                )
            }
        }
    }
}

private struct QuestionRow: View {
    let question: QuestionResult

    let isSelected: Bool

    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 10) {
                Text("Q\(question.questionNo)")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.primaryDeep)
                    .frame(width: 30, height: 24)
                    .background(PassmateColors.fieldGray)
                    .cornerRadius(8)
                Text(question.title)
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.textPrimary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                VerdictChip(verdict: question.verdict)
                Text("›")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(PassmateColors.textSecondary)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(PassmateColors.surface)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(isSelected ? PassmateColors.primary : PassmateColors.border, lineWidth: 1)
            )
            .cornerRadius(16)
        }
    }
}

private struct VerdictChip: View {
    let verdict: AnswerVerdict

    var body: some View {
        let style = verdictStyle(verdict)

        Text(style.label)
            .font(.system(size: 14, weight: .medium))
            .kerning(-0.28)
            .foregroundColor(style.textColor)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(style.background)
            .clipShape(Capsule())
    }

    private func verdictStyle(_ verdict: AnswerVerdict) -> (background: Color, textColor: Color, label: String) {
        if verdict == AnswerVerdict.correct {
            return (PassmateColors.chipGreen, PassmateColors.chipGreenText, "정답")
        } else if verdict == AnswerVerdict.wrong {
            return (PassmateColors.wrongPink, PassmateColors.wrongPinkText, "오답")
        } else if verdict == AnswerVerdict.aiAnalyzed {
            return (PassmateColors.chipGold, PassmateColors.chipGoldText, "AI 분석")
        } else if verdict == AnswerVerdict.aiPending {
            return (PassmateColors.chipGold, PassmateColors.chipGoldText, "분석 중")
        } else {
            return (PassmateColors.fieldGray, PassmateColors.textSecondary, "미채점")
        }
    }
}

// UIActivityViewController 래퍼 — 리포트 요약 텍스트 공유
private struct ShareSheet: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

// 칩·주제 태그를 가로로 흐르게 배치하는 간단한 FlowLayout (iOS 16+)
private struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout Void) -> CGSize {
        let maxWidth = proposal.width ?? .infinity
        var rowWidth: CGFloat = 0
        var rowHeight: CGFloat = 0
        var totalHeight: CGFloat = 0
        var totalWidth: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)

            if rowWidth + size.width > maxWidth, rowWidth > 0 {
                totalHeight += rowHeight + spacing
                totalWidth = max(totalWidth, rowWidth - spacing)
                rowWidth = 0
                rowHeight = 0
            }
            rowWidth += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
        totalHeight += rowHeight
        totalWidth = max(totalWidth, rowWidth - spacing)

        return CGSize(width: min(totalWidth, maxWidth), height: totalHeight)
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

struct ResultView_Previews: PreviewProvider {
    static var previews: some View {
        ResultView(roomId: 1)
    }
}
