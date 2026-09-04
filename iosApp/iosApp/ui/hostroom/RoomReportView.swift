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

// 정답률 구간 1칸 — 라벨·인원·막대 색 (v6 M-14 개요 "정답률 분포")
private struct AccuracyBand: Identifiable {
    let id: Int

    let label: String

    let count: Int

    let color: Color
}

private struct RoomReportContentView: View {
    static let mostMissedLimit = 3

    static let topRankLimit = 3

    static let accuracyBandLabels = ["0~40%", "41~60%", "61~80%", "81~100%"]

    static let accuracyBandColors: [Color] = [
        PassmateColors.wrongPink,
        PassmateColors.accuracyBandMid,
        PassmateColors.ratingTagSelectedBg,
        PassmateColors.primary
    ]

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

    // 개요 탭 — 정답률 분포 · 많이 틀린 문항 TOP 3 · 요약 (v6 M-14 개요)
    // 시안의 "AI 총평" 카드는 계약(RoomReportResponse)에 총평 텍스트 필드가 없어 제외하고, 같은 자리에 요약 카드를 둔다
    private func overviewTab(_ report: RoomReport) -> some View {
        VStack(spacing: 14) {
            accuracyDistributionCard(report)
            mostMissedQuestionsCard(report)
            overviewSummaryCard(report)
        }
    }

    // 정답률 분포 — 서버가 준 학생별 정답 수를 4구간으로 묶어 보여준다 (점수·정오 판정은 하지 않는다)
    private func accuracyDistributionCard(_ report: RoomReport) -> some View {
        let bands = accuracyBands(students: reportStudents(report), questionCount: Int(report.summary.questionCount))
        let bandTotal = bands.reduce(0) { $0 + $1.count }

        return VStack(spacing: 14) {
            cardHeaderRow(title: "정답률 분포") {
                Text("학생 \(report.summary.studentCount)명")
                    .font(.system(size: 12))
                    .kerning(-0.12)
                    .foregroundColor(PassmateColors.textTertiary)
            }
            if bandTotal == 0 {
                emptyTabText("정답률 분포를 계산할 수 없어요")
            } else {
                ForEach(bands) { band in
                    accuracyBandRow(band: band, total: bandTotal)
                }
            }
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 16)
        .background(PassmateColors.surface)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(PassmateColors.border, lineWidth: 1))
    }

    private func accuracyBandRow(band: AccuracyBand, total: Int) -> some View {
        HStack(spacing: 8) {
            Text(band.label)
                .font(.system(size: 12, weight: .medium))
                .kerning(-0.12)
                .foregroundColor(PassmateColors.textSecondary)
                .frame(width: 60, alignment: .leading)
            GeometryReader { geo in
                let fraction = CGFloat(band.count) / CGFloat(total)

                ZStack(alignment: .leading) {
                    Capsule().fill(PassmateColors.fieldGray)
                    if fraction > 0 {
                        Capsule()
                            .fill(band.color)
                            .frame(width: geo.size.width * fraction)
                    }
                }
            }
            .frame(height: 10)
            Text("\(band.count)명")
                .font(.system(size: 12, weight: .bold))
                .kerning(-0.12)
                .foregroundColor(PassmateColors.textPrimary)
                .frame(width: 40, alignment: .trailing)
        }
    }

    // 많이 틀린 문항 TOP 3 — 서버가 준 정답률의 여집합을 오답률로 표시한다 (미채점 서술형은 제외)
    private func mostMissedQuestionsCard(_ report: RoomReport) -> some View {
        let mostMissed = reportQuestions(report)
            .filter { $0.accuracyPercent != nil }
            .sorted { ($0.accuracyPercent?.intValue ?? 0) < ($1.accuracyPercent?.intValue ?? 0) }
            .prefix(Self.mostMissedLimit)

        return VStack(spacing: 14) {
            cardHeaderRow(title: "많이 틀린 문항 TOP \(Self.mostMissedLimit)") {
                Button {
                    onAction(.selectTab(tab: .questions))
                } label: {
                    Text("문항별 ›")
                        .font(.system(size: 12, weight: .bold))
                        .kerning(-0.12)
                        .foregroundColor(PassmateColors.primaryDeep)
                }
            }
            if mostMissed.isEmpty {
                emptyTabText("채점된 문항이 아직 없어요")
            } else {
                ForEach(Array(mostMissed), id: \.questionId) { question in
                    mostMissedRow(question)
                }
            }
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 16)
        .background(PassmateColors.surface)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(PassmateColors.border, lineWidth: 1))
    }

    private func mostMissedRow(_ question: ReportQuestion) -> some View {
        let accuracyPercent = question.accuracyPercent?.intValue ?? 0

        return HStack(spacing: 8) {
            Text("Q\(question.questionNo)")
                .font(.system(size: 12, weight: .bold))
                .kerning(-0.12)
                .foregroundColor(PassmateColors.textSecondary)
                .frame(width: 30, height: 22)
                .background(PassmateColors.fieldGray)
                .cornerRadius(6)
            Text(question.title)
                .font(.system(size: 13, weight: .medium))
                .kerning(-0.13)
                .foregroundColor(PassmateColors.textPrimary)
                .lineLimit(1)
                .frame(maxWidth: .infinity, alignment: .leading)
            Text("오답 \(100 - accuracyPercent)%")
                .font(.system(size: 12, weight: .bold))
                .kerning(-0.12)
                .foregroundColor(PassmateColors.wrongPinkText)
        }
    }

    private func overviewSummaryCard(_ report: RoomReport) -> some View {
        let questions = reportQuestions(report)
        let choiceCount = questions.filter { $0.type == QuestionType.multipleChoice }.count
        let oxCount = questions.filter { $0.type == QuestionType.ox }.count
        let essayCount = questions.filter { $0.type == QuestionType.essay }.count

        return VStack(spacing: 10) {
            overviewRow(label: "평균 점수", value: report.summary.avgScore.map { "\(formatScore($0.doubleValue))점" } ?? "—")
            overviewRow(label: "최고 점수", value: report.summary.topScore.map { "\(formatScore($0.doubleValue))점" } ?? "—")
            overviewRow(label: "문항 구성", value: "객관식 \(choiceCount) · OX \(oxCount) · 서술형 \(essayCount)")
            overviewRow(label: "AI 분석", value: "\(report.summary.aiAnalysisCount)건")
        }
        .padding(16)
        .background(PassmateColors.surface)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(PassmateColors.border, lineWidth: 1))
    }

    private func cardHeaderRow<Trailing: View>(
        title: String,
        @ViewBuilder trailing: () -> Trailing
    ) -> some View {
        HStack {
            Text(title)
                .font(.system(size: 15, weight: .bold))
                .kerning(-0.15)
                .foregroundColor(PassmateColors.textPrimary)
            Spacer()
            trailing()
        }
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

    // 학생별 탭 — 학생 수·정렬 칩 + 순위/정답 수/점수 목록 (v6 M-14 학생별)
    // 시안의 "제출 N" · "미제출 N명" 카드는 계약에 제출 여부 필드가 없어 제외한다
    private func studentsTab(_ report: RoomReport) -> some View {
        let students = sortedStudents(students: reportStudents(report), sort: uiState.studentSort)

        return VStack(spacing: 10) {
            HStack {
                Text("학생 \(report.summary.studentCount)명")
                    .font(.system(size: 13, weight: .bold))
                    .kerning(-0.13)
                    .foregroundColor(PassmateColors.textPrimary)
                Spacer()
                HStack(spacing: 8) {
                    ForEach(StudentSort.allCases, id: \.self) { sort in
                        studentSortChip(sort: sort, isSelected: sort == uiState.studentSort)
                    }
                }
            }
            VStack(spacing: 0) {
                if students.isEmpty {
                    emptyTabText("참가 학생이 없어요")
                }
                ForEach(Array(students.enumerated()), id: \.element.participantId) { index, student in
                    if index > 0 {
                        Divider().background(PassmateColors.border)
                    }
                    studentRow(student, questionCount: Int(report.summary.questionCount))
                }
            }
            .background(PassmateColors.surface)
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(PassmateColors.border, lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
    }

    private func studentSortChip(sort: StudentSort, isSelected: Bool) -> some View {
        Button {
            onAction(.selectStudentSort(sort: sort))
        } label: {
            Text(sort.label)
                .font(.system(size: 12, weight: isSelected ? .bold : .medium))
                .kerning(-0.12)
                .foregroundColor(isSelected ? PassmateColors.textSecondary : PassmateColors.textTertiary)
                .frame(width: 66, height: 30)
                .background(isSelected ? PassmateColors.fieldGray : PassmateColors.surface)
                .clipShape(Capsule())
                .overlay(
                    Capsule().stroke(isSelected ? PassmateColors.fieldGray : PassmateColors.border, lineWidth: 1)
                )
        }
    }

    private func studentRow(_ student: ReportStudent, questionCount: Int) -> some View {
        let rank = student.rank?.intValue
        let isTopRank = rank != nil && rank! <= Self.topRankLimit

        return HStack(spacing: 12) {
            Text(rank.map { "\($0)" } ?? "-")
                .font(.system(size: 12, weight: .bold))
                .kerning(-0.12)
                .foregroundColor(isTopRank ? PassmateColors.primaryDeep : PassmateColors.textTertiary)
                .frame(width: 26, height: 26)
                .background(isTopRank ? PassmateColors.backgroundMint : PassmateColors.fieldGray)
                .clipShape(Circle())
            VStack(alignment: .leading, spacing: 3) {
                HStack(spacing: 6) {
                    Text(student.nickname)
                        .font(.system(size: 14, weight: .bold))
                        .kerning(-0.14)
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
                }
                Text("정답 \(student.correctCount)/\(questionCount)")
                    .font(.system(size: 12))
                    .kerning(-0.12)
                    .foregroundColor(PassmateColors.textSecondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Text("\(formatScore(student.totalScore))점")
                .font(.system(size: 13, weight: .bold))
                .kerning(-0.13)
                .foregroundColor(isTopRank ? PassmateColors.primary : PassmateColors.textSecondary)
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

    private func accuracyBands(students: [ReportStudent], questionCount: Int) -> [AccuracyBand] {
        var counts = [Int](repeating: 0, count: Self.accuracyBandLabels.count)

        if questionCount > 0 {
            for student in students {
                let percent = Int(student.correctCount) * 100 / questionCount
                let index: Int

                if percent <= 40 {
                    index = 0
                } else if percent <= 60 {
                    index = 1
                } else if percent <= 80 {
                    index = 2
                } else {
                    index = 3
                }
                counts[index] += 1
            }
        }

        return Self.accuracyBandLabels.enumerated().map { index, label in
            AccuracyBand(id: index, label: label, count: counts[index], color: Self.accuracyBandColors[index])
        }
    }

    private func sortedStudents(students: [ReportStudent], sort: StudentSort) -> [ReportStudent] {
        switch sort {
        // 순위는 서버 값 — 점수순은 서버 순위를 그대로 따르고, 순위가 없는 학생만 점수 내림차순으로 뒤에 둔다
        case .score:
            return students.sorted { lhs, rhs in
                let lhsRank = lhs.rank?.intValue ?? Int.max
                let rhsRank = rhs.rank?.intValue ?? Int.max

                if lhsRank == rhsRank {
                    return lhs.totalScore > rhs.totalScore
                } else {
                    return lhsRank < rhsRank
                }
            }
        case .name:
            return students.sorted { $0.nickname < $1.nickname }
        }
    }

    private func formatScore(_ score: Double) -> String {
        let digits = String(Int(score))

        return String(
            digits.reversed().enumerated().map { index, char -> String in
                index > 0 && index % 3 == 0 ? ",\(char)" : String(char)
            }
            .joined()
            .reversed()
        )
    }

    // 시안 M-14: "8/22(금) 진행 · 종료된 방 · PIN 482 913".
    // 상태는 서버 값을 따르고, pin은 서버가 주지 않을 때(빈 값) 조각을 통째로 생략한다
    private func subtitle(_ report: RoomReport) -> String {
        var parts: [String] = []

        if let dateLabel = report.dateLabel {
            parts.append("\(dateLabel) 진행")
        }
        if let status = statusLabel(report.status) {
            parts.append(status)
        }
        if !report.pin.isEmpty {
            parts.append("PIN \(formatPin(report.pin))")
        }
        return parts.joined(separator: " · ")
    }

    private func statusLabel(_ status: RoomStatus) -> String? {
        switch status {
        case .waiting:
            return "대기 중인 방"
        case .running:
            return "진행 중인 방"
        case .finished:
            return "종료된 방"
        default:
            return nil
        }
    }

    private func formatPin(_ pin: String) -> String {
        stride(from: 0, to: pin.count, by: 3).map { start in
            let begin = pin.index(pin.startIndex, offsetBy: start)
            let end = pin.index(begin, offsetBy: min(3, pin.count - start))
            return String(pin[begin..<end])
        }.joined(separator: " ")
    }
}
