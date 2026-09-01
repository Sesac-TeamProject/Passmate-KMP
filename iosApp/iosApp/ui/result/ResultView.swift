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
        submitRatingUseCase: KoinHelper.shared.submitRatingUseCase(),
        eventWatcher: KoinHelper.shared.sessionEventStreamWatcher()
    )

    @State private var shareText: String?

    @State private var noticeMessage: String?

    var body: some View {
        ResultContentView(
            uiState: viewModel.uiState,
            onAction: { viewModel.action($0) },
            // Result의 뒤로가기는 세션 플로우 엔트리를 지나 탭 루트로 돌아간다 (규칙 §2-1-2)
            onBack: onClickHome
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
            case .ratingSubmitted:
                break
            case let .showNotice(message):
                noticeMessage = message
            }
        }
        .overlay(alignment: .bottom) {
            if let noticeMessage {
                ResultNoticeToast(message: noticeMessage)
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                            self.noticeMessage = nil
                        }
                    }
            }
        }
        // 평가 시트는 컨테이너가 소유 (규칙 §11-1)
        .sheet(isPresented: Binding(
            get: { viewModel.uiState.isRatingSheetVisible },
            set: { if !$0 { viewModel.action(.dismissRatingSheet) } }
        )) {
            RatingSectionView(
                uiState: viewModel.uiState,
                onAction: { viewModel.action($0) }
            )
            .passmateDetents([.large])
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

    let onBack: () -> Void

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

    // 시안 M-06e 불러오기 실패 — 상단 경고 바·헤더·알림 아이콘·안내 문구·재시도/문의 버튼
    private var errorView: some View {
        VStack(spacing: 0) {
            Rectangle()
                .fill(PassmateColors.wrongPink)
                .frame(height: 3)
            errorHeader
            VStack(spacing: 0) {
                alertCircleIcon
                Text("리포트를 불러오지 못했어요")
                    .font(.system(size: 22, weight: .bold))
                    .kerning(-0.22)
                    .multilineTextAlignment(.center)
                    .foregroundColor(PassmateColors.textPrimary)
                    .padding(.top, 20)
                Text("잠시 후 다시 시도해 주세요.\n계속 안 되면 방이 삭제됐을 수 있어요.")
                    .font(.system(size: 15))
                    .lineSpacing(15 * 0.65)
                    .multilineTextAlignment(.center)
                    .foregroundColor(PassmateColors.textSecondary)
                    .padding(.top, 10)
                Text("이미 저장된 리포트는\n마이 › 참여한 방에서 볼 수 있어요")
                    .font(.system(size: 14))
                    .lineSpacing(14 * 0.65)
                    .multilineTextAlignment(.center)
                    .foregroundColor(PassmateColors.textTertiary)
                    .padding(.top, 16)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .padding(.horizontal, 20)
            retryButton
            contactButton
                .padding(.top, 10)
                .padding(.bottom, 24)
        }
    }

    private var errorHeader: some View {
        HStack(spacing: 12) {
            Button(action: onBack) {
                Text("←")
                    .font(.system(size: 20))
                    .foregroundColor(PassmateColors.textPrimary)
            }
            Text("리포트")
                .font(.system(size: 15, weight: .bold))
                .kerning(-0.3)
                .foregroundColor(PassmateColors.textPrimary)
            Spacer()
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 14)
    }

    // alert-circle — 원형 배경 위 외곽선 원 + 느낌표 (아이콘 에셋 없이 기본 도형으로 구성)
    private var alertCircleIcon: some View {
        ZStack {
            Circle()
                .fill(PassmateColors.errorIconBg)
            Circle()
                .stroke(PassmateColors.wrongPinkText, lineWidth: 2)
                .frame(width: 28, height: 28)
            Text("!")
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(PassmateColors.wrongPinkText)
        }
        .frame(width: 64, height: 64)
    }

    private var retryButton: some View {
        Button {
            onAction(.retry)
        } label: {
            Text("다시 시도")
                .font(.system(size: 16, weight: .bold))
                .kerning(-0.32)
                .foregroundColor(PassmateColors.surface)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(PassmateColors.primary)
                .cornerRadius(14)
        }
        .padding(.horizontal, 20)
    }

    private var contactButton: some View {
        Button {
            onAction(.clickContactSupport)
        } label: {
            Text("문의하기")
                .font(.system(size: 16, weight: .bold))
                .kerning(-0.32)
                .foregroundColor(PassmateColors.textPrimary)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(PassmateColors.surface)
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(PassmateColors.border, lineWidth: 1.5))
                .cornerRadius(14)
        }
        .padding(.horizontal, 20)
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

// 단발 안내 토스트 — Compose ResultScreen의 SnackbarHost 미러
private struct ResultNoticeToast: View {
    let message: String

    var body: some View {
        Text(message)
            .font(.system(size: 13))
            .foregroundColor(PassmateColors.surface)
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(PassmateColors.textPrimary.opacity(0.9))
            .cornerRadius(10)
            .padding(.bottom, 16)
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

// MARK: - 프리뷰 (Figma 시안 비교용, Koin 미초기화 상태에서도 안전한 콘텐츠 뷰 기반)

#Preview("데이터 (정답 6/8 · 3위 · 990점)") {
    ResultContentView(
        uiState: ResultUiState(
            isLoading: false,
            result: SessionResult(
                roomTitle: "8월 4주차 Spring 스터디",
                rank: KotlinInt(int: 3),
                totalScore: 990,
                correctCount: 6,
                questionCount: 8,
                questions: [
                    QuestionResult(
                        questionId: 1,
                        questionNo: 1,
                        title: "등차수열의 공차",
                        type: .multipleChoice,
                        verdict: .correct,
                        myAnswer: "3",
                        correctAnswer: "3",
                        explanation: "이웃한 항의 차가 3으로 일정해요.",
                        earnedScore: 120,
                        aiFeedback: nil,
                        hostReview: nil
                    ),
                    QuestionResult(
                        questionId: 2,
                        questionNo: 2,
                        title: "이차함수의 최댓값 OX",
                        type: .ox,
                        verdict: .wrong,
                        myAnswer: "O",
                        correctAnswer: "X",
                        explanation: "아래로 볼록한 이차함수는 최댓값이 없어요.",
                        earnedScore: 0,
                        aiFeedback: nil,
                        hostReview: nil
                    ),
                    QuestionResult(
                        questionId: 3,
                        questionNo: 3,
                        title: "이차방정식의 판별식 활용 서술형",
                        type: .essay,
                        verdict: .aiAnalyzed,
                        myAnswer: "판별식 D = b^2 - 4ac를 이용해 근의 개수를 구했습니다.",
                        correctAnswer: nil,
                        explanation: nil,
                        earnedScore: 85,
                        aiFeedback: AiFeedback(
                            status: .done,
                            coveredConcepts: ["판별식 공식", "근의 개수 판정"],
                            missingConcepts: ["중근 조건 설명"],
                            weaknesses: nil,
                            improvement: "부호 판정 과정을 한 단계 더 풀어써 주면 좋아요",
                            suggestedScore: KotlinDouble(double: 85)
                        ),
                        hostReview: nil
                    )
                ],
                canRate: true,
                isGuest: false
            ),
            report: LearningReport(
                accuracyPercent: 75,
                weakTopics: ["이차함수", "확률과 통계"],
                improvementPoints: ["판별식 부호 판정 연습이 필요해요"]
            ),
            selectedQuestionNo: 3
        ),
        onAction: { _ in },
        onBack: {}
    )
}

#Preview("로딩 중") {
    ResultContentView(
        uiState: ResultUiState(isLoading: true),
        onAction: { _ in },
        onBack: {}
    )
}

#Preview("불러오기 실패") {
    ResultContentView(
        uiState: ResultUiState(isLoading: false, loadFailed: true),
        onAction: { _ in },
        onBack: {}
    )
}
