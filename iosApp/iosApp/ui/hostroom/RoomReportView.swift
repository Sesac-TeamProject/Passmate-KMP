import SwiftUI
import Shared

// Figma "UI 디자인 v6" M-14(432:5366) 미러 — 방 리포트: 요약 카드+개요/문항별/학생별 탭+내보내기(텍스트 공유)
struct RoomReportView: View {
    let roomId: Int64

    var onRequireSignIn: () -> Void = {}

    var onBack: () -> Void = {}

    @StateObject private var viewModel = RoomReportViewModel(
        getRoomReportUseCase: KoinHelper.shared.getRoomReportUseCase(),
        buildRoomReportSummaryUseCase: KoinHelper.shared.buildRoomReportSummaryUseCase(),
        isSignedInUseCase: KoinHelper.shared.isSignedInUseCase()
    )

    @State private var shareText: String?

    var body: some View {
        RoomReportContentView(
            uiState: viewModel.uiState,
            onAction: { viewModel.action($0) },
            onRetry: { viewModel.action(.retry(roomId: roomId)) },
            onClickBack: onBack
        )
        .onAppear {
            viewModel.action(.enter(roomId: roomId))
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .requireSignIn:
                onRequireSignIn()
            case let .shareReport(summary):
                shareText = summary
            }
        }
        .sheet(isPresented: Binding(get: { shareText != nil }, set: { if !$0 { shareText = nil } })) {
            if let shareText {
                ReportShareSheet(items: [shareText])
            }
        }
    }
}

// UIActivityViewController 래퍼 — 방 리포트 요약 텍스트 공유
private struct ReportShareSheet: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

private struct RoomReportContentView: View {
    let uiState: RoomReportUiState

    let onAction: (RoomReportAction) -> Void

    let onRetry: () -> Void

    let onClickBack: () -> Void

    var body: some View {
        Group {
            if uiState.isLoading {
                ProgressView()
                    .tint(PassmateColors.primary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if uiState.loadFailed || uiState.report == nil {
                errorView
            } else if let report = uiState.report {
                loadedView(report)
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
            Button(action: onRetry) {
                Text("다시 시도")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.primaryDeep)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func loadedView(_ report: RoomReport) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                HStack(spacing: 12) {
                    PassmateBackButton(onClick: onClickBack)
                    Text("방 리포트")
                        .font(.system(size: 18, weight: .bold))
                        .kerning(-0.36)
                        .foregroundColor(PassmateColors.textPrimary)
                    Spacer()
                    Button(action: { onAction(.clickExport) }) {
                        Text("내보내기")
                            .font(.system(size: 14, weight: .medium))
                            .kerning(-0.28)
                            .foregroundColor(PassmateColors.primaryDeep)
                    }
                }
                VStack(alignment: .leading, spacing: 4) {
                    Text(report.roomTitle)
                        .font(.system(size: 22, weight: .bold))
                        .kerning(-0.44)
                        .foregroundColor(PassmateColors.textPrimary)
                    Text(subtitle(report))
                        .font(.system(size: 13))
                        .kerning(-0.26)
                        .foregroundColor(PassmateColors.textSecondary)
                }
                statCards(report)
                tabChips
                switch uiState.selectedTab {
                case .overview:
                    overviewTab(report)
                case .questions:
                    questionsTab(report)
                case .students:
                    studentsTab(report)
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 24)
            .padding(.bottom, 24)
        }
    }

    private func statCards(_ report: RoomReport) -> some View {
        VStack(spacing: 10) {
            HStack(spacing: 10) {
                statCard(value: report.summary.avgAccuracyPercent.map { "\($0)%" } ?? "—", label: "평균 정답률")
                statCard(value: "\(report.summary.studentCount)명", label: "참가 학생")
            }
            HStack(spacing: 10) {
                statCard(value: "\(report.summary.questionCount)개", label: "문항")
                statCard(value: "\(report.summary.aiAnalysisCount)건", label: "AI 분석")
            }
        }
    }

    private func statCard(value: String, label: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(value)
                .font(.system(size: 20, weight: .bold))
                .kerning(-0.4)
                .foregroundColor(PassmateColors.primaryDeep)
            Text(label)
                .font(.system(size: 13))
                .kerning(-0.26)
                .foregroundColor(PassmateColors.textSecondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(PassmateColors.surface)
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(PassmateColors.border, lineWidth: 1))
    }

    private var tabChips: some View {
        HStack(spacing: 8) {
            ForEach(ReportTab.allCases, id: \.self) { tab in
                let isSelected = tab == uiState.selectedTab

                Button(action: { onAction(.selectTab(tab: tab)) }) {
                    Text(tab.label)
                        .font(.system(size: 13, weight: .medium))
                        .kerning(-0.26)
                        .foregroundColor(isSelected ? PassmateColors.ratingTagSelectedText : PassmateColors.textSecondary)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                        .background(isSelected ? PassmateColors.ratingTagSelectedBg : PassmateColors.fieldGray)
                        .clipShape(Capsule())
                }
            }
        }
    }

    private func overviewTab(_ report: RoomReport) -> some View {
        let questions = reportQuestions(report)
        let choiceCount = questions.filter { $0.type == QuestionType.multipleChoice }.count
        let oxCount = questions.filter { $0.type == QuestionType.ox }.count
        let essayCount = questions.filter { $0.type == QuestionType.essay }.count

        return VStack(spacing: 10) {
            overviewRow(label: "평균 점수", value: report.summary.avgScore.map { "\(Int($0.doubleValue))점" } ?? "—")
            overviewRow(label: "최고 점수", value: report.summary.topScore.map { "\(Int($0.doubleValue))점" } ?? "—")
            overviewRow(label: "문항 구성", value: "객관식 \(choiceCount) · OX \(oxCount) · 서술형 \(essayCount)")
            overviewRow(label: "AI 분석", value: "\(report.summary.aiAnalysisCount)건")
        }
        .padding(16)
        .background(PassmateColors.surface)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(PassmateColors.border, lineWidth: 1))
    }

    private func overviewRow(label: String, value: String) -> some View {
        HStack {
            Text(label)
                .font(.system(size: 14))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.textSecondary)
            Spacer()
            Text(value)
                .font(.system(size: 14, weight: .medium))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.textPrimary)
        }
    }

    private func questionsTab(_ report: RoomReport) -> some View {
        VStack(spacing: 0) {
            let questions = reportQuestions(report)

            if questions.isEmpty {
                emptyTabText("문항 통계가 없어요")
            }
            ForEach(Array(questions.enumerated()), id: \.element.questionId) { index, question in
                if index > 0 {
                    Divider().background(PassmateColors.border)
                }
                questionRow(question)
            }
        }
        .background(PassmateColors.surface)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(PassmateColors.border, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func questionRow(_ question: ReportQuestion) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Text("Q\(question.questionNo)")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundColor(PassmateColors.textSecondary)
                    .frame(width: 28, height: 28)
                    .background(PassmateColors.fieldGray)
                    .clipShape(Circle())
                Text(question.title)
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.textPrimary)
                Spacer()
                if let count = question.aiFeedbackCount?.intValue {
                    Text("AI 분석 \(count)건")
                        .font(.system(size: 12, weight: .medium))
                        .kerning(-0.24)
                        .foregroundColor(PassmateColors.primaryDeep)
                } else if question.type == QuestionType.essay {
                    Text("서술형")
                        .font(.system(size: 12, weight: .medium))
                        .kerning(-0.24)
                        .foregroundColor(PassmateColors.primaryDeep)
                }
                Text(question.accuracyPercent.map { "\($0)%" } ?? "—")
                    .font(.system(size: 13, weight: .medium))
                    .kerning(-0.26)
                    .foregroundColor(PassmateColors.textSecondary)
            }
            accuracyBar(percent: question.accuracyPercent?.intValue)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }

    private func accuracyBar(percent: Int?) -> some View {
        GeometryReader { geo in
            let fraction = CGFloat(min(max(percent ?? 0, 0), 100)) / 100

            ZStack(alignment: .leading) {
                Capsule().fill(PassmateColors.fieldGray)
                if fraction > 0 {
                    Capsule()
                        .fill(PassmateColors.primary)
                        .frame(width: geo.size.width * fraction)
                }
            }
        }
        .frame(height: 8)
    }

    private func studentsTab(_ report: RoomReport) -> some View {
        VStack(spacing: 0) {
            let students = reportStudents(report)

            if students.isEmpty {
                emptyTabText("참가 학생이 없어요")
            }
            ForEach(Array(students.enumerated()), id: \.element.participantId) { index, student in
                if index > 0 {
                    Divider().background(PassmateColors.border)
                }
                studentRow(student)
            }
        }
        .background(PassmateColors.surface)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(PassmateColors.border, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func studentRow(_ student: ReportStudent) -> some View {
        HStack(spacing: 10) {
            Text(student.rank?.stringValue ?? "-")
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(PassmateColors.textSecondary)
                .frame(width: 26, height: 26)
                .background(PassmateColors.fieldGray)
                .clipShape(Circle())
            Text(student.nickname)
                .font(.system(size: 14, weight: .medium))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.textPrimary)
            if student.isGuest {
                Text("게스트")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundColor(PassmateColors.textTertiary)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(PassmateColors.fieldGray)
                    .clipShape(Capsule())
            }
            Spacer()
            Text("정답 \(student.correctCount)")
                .font(.system(size: 13))
                .kerning(-0.26)
                .foregroundColor(PassmateColors.textSecondary)
            Text("\(Int(student.totalScore))점")
                .font(.system(size: 14, weight: .bold))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.textPrimary)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }

    private func emptyTabText(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 14))
            .kerning(-0.28)
            .foregroundColor(PassmateColors.textSecondary)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 32)
    }

    private func reportQuestions(_ report: RoomReport) -> [ReportQuestion] {
        report.questions.compactMap { $0 as? ReportQuestion }
    }

    private func reportStudents(_ report: RoomReport) -> [ReportStudent] {
        report.students.compactMap { $0 as? ReportStudent }
    }

    private func subtitle(_ report: RoomReport) -> String {
        var parts: [String] = []

        if let dateLabel = report.dateLabel {
            parts.append("\(dateLabel) 진행")
        }
        parts.append("종료된 방")
        parts.append("PIN \(formatPin(report.pin))")

        return parts.joined(separator: " · ")
    }

    private func formatPin(_ pin: String) -> String {
        stride(from: 0, to: pin.count, by: 3).map { start in
            let begin = pin.index(pin.startIndex, offsetBy: start)
            let end = pin.index(begin, offsetBy: min(3, pin.count - start))
            return String(pin[begin..<end])
        }.joined(separator: " ")
    }
}
