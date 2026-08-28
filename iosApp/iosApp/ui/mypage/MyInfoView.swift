import SwiftUI
import Shared

// Figma "UI 디자인 v6" M-08(349:9544) 미러 — 진행 중 방·누적 요약·보완 주제·참여한 방 목록(→리포트) (T064)
struct MyInfoView: View {
    var onRequireSignIn: () -> Void = {}

    var onOpenReport: (Int64) -> Void = { _ in }

    var onRejoin: (String) -> Void = { _ in }

    var onBack: () -> Void = {}

    @StateObject private var viewModel = MyInfoViewModel(
        getMyPageUseCase: KoinHelper.shared.getMyPageUseCase(),
        isSignedInUseCase: KoinHelper.shared.isSignedInUseCase()
    )

    @State private var noticeMessage: String?

    var body: some View {
        MyInfoContentView(
            uiState: viewModel.uiState,
            onAction: { viewModel.action($0) },
            onClickBack: onBack
        )
        .onAppear {
            viewModel.action(.enter)
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .requireSignIn:
                onRequireSignIn()
            case let .openReport(roomId):
                onOpenReport(roomId)
            case let .rejoin(pin):
                onRejoin(pin)
            case let .showNotice(message):
                noticeMessage = message
            }
        }
        .overlay(alignment: .bottom) {
            if let noticeMessage {
                MyInfoNoticeToast(message: noticeMessage)
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                            self.noticeMessage = nil
                        }
                    }
            }
        }
    }
}

private struct MyInfoContentView: View {
    let uiState: MyInfoUiState

    let onAction: (MyInfoAction) -> Void

    let onClickBack: () -> Void

    var body: some View {
        Group {
            if uiState.isLoading {
                ProgressView()
                    .tint(PassmateColors.primary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if uiState.loadFailed {
                errorView
            } else {
                loadedView
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(PassmateColors.surface.ignoresSafeArea())
    }

    private var errorView: some View {
        VStack(spacing: 12) {
            Text("기록을 불러오지 못했어요")
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

    private var loadedView: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                HStack {
                    Text("참여한 방")
                        .font(.system(size: 24, weight: .bold))
                        .kerning(-0.48)
                        .foregroundColor(PassmateColors.textPrimary)
                    Spacer()
                    Button(action: onClickBack) {
                        Text("닫기")
                            .font(.system(size: 14, weight: .medium))
                            .kerning(-0.28)
                            .foregroundColor(PassmateColors.textSecondary)
                    }
                }
                if let ongoing = uiState.ongoing {
                    OngoingCard(ongoing: ongoing, onClickRejoin: { onAction(.clickRejoin(pin: ongoing.pin)) })
                }
                if let summary = uiState.summary {
                    SummaryCard(summary: summary)
                    WeakTopicsRow(topics: summary.weakTopics)
                }
                if uiState.rooms.isEmpty, uiState.ongoing == nil {
                    Text("아직 참여한 방이 없어요")
                        .font(.system(size: 14, weight: .medium))
                        .kerning(-0.28)
                        .foregroundColor(PassmateColors.textSecondary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 40)
                } else {
                    ForEach(uiState.rooms, id: \.roomId) { room in
                        JoinedRoomRow(room: room, onClickReport: { onAction(.clickRoomReport(roomId: room.roomId)) })
                    }
                }
                if uiState.nextCursor != nil {
                    loadMoreButton
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 32)
            .padding(.bottom, 24)
        }
    }

    private var loadMoreButton: some View {
        Button {
            onAction(.loadMore)
        } label: {
            Group {
                if uiState.isLoadingMore {
                    ProgressView().tint(PassmateColors.primary)
                } else {
                    Text("더 보기")
                        .font(.system(size: 14, weight: .medium))
                        .kerning(-0.28)
                        .foregroundColor(PassmateColors.primaryDeep)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 48)
            .overlay(RoundedRectangle(cornerRadius: 14).stroke(PassmateColors.border, lineWidth: 1))
        }
        .disabled(uiState.isLoadingMore)
    }
}

private struct OngoingCard: View {
    let ongoing: OngoingRoom

    let onClickRejoin: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                Text("진행 중")
                    .font(.system(size: 12))
                    .kerning(-0.24)
                    .foregroundColor(PassmateColors.surface)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(PassmateColors.primary)
                    .clipShape(Capsule())
                Text(ongoing.title)
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.textPrimary)
            }
            Text(subtitle)
                .font(.system(size: 14))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.textSecondary)
            Button(action: onClickRejoin) {
                Text("다시 들어가기")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.surface)
                    .frame(maxWidth: .infinity)
                    .frame(height: 44)
                    .background(PassmateColors.primary)
                    .cornerRadius(12)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(PassmateColors.backgroundMint)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(PassmateColors.primary, lineWidth: 1))
        .cornerRadius(16)
    }

    private var subtitle: String {
        var parts: [String] = []

        if let progress = ongoing.progressLabel { parts.append(progress) }
        if let host = ongoing.hostNickname { parts.append("\(host) 선생님") }
        parts.append("PIN \(formatPin(ongoing.pin))")

        return parts.joined(separator: " · ")
    }
}

private struct SummaryCard: View {
    let summary: MyPageSummary

    var body: some View {
        HStack(spacing: 16) {
            ZStack {
                Circle()
                    .stroke(PassmateColors.primary, lineWidth: 6)
                VStack(spacing: 0) {
                    Text("\(summary.accuracyPercent)%")
                        .font(.system(size: 16, weight: .medium))
                        .kerning(-0.32)
                        .foregroundColor(PassmateColors.primaryDeep)
                    Text("평균")
                        .font(.system(size: 14, weight: .medium))
                        .kerning(-0.28)
                        .foregroundColor(PassmateColors.textSecondary)
                }
            }
            .frame(width: 70, height: 70)
            VStack(alignment: .leading, spacing: 3) {
                Text(summaryLine)
                    .font(.system(size: 16, weight: .medium))
                    .kerning(-0.32)
                    .foregroundColor(PassmateColors.textPrimary)
                if let trend = summary.trendText {
                    Text(trend)
                        .font(.system(size: 14, weight: .medium))
                        .kerning(-0.28)
                        .foregroundColor(PassmateColors.textSecondary)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 16)
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(PassmateColors.border, lineWidth: 1))
        .cornerRadius(20)
    }

    private var summaryLine: String {
        let rankPart = summary.avgRank.map { " · 평균 \(formatRank(Double(truncating: $0)))위" } ?? ""

        return "\(summary.participationCount)회 참여\(rankPart)"
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

private struct JoinedRoomRow: View {
    let room: JoinedRoom

    let onClickReport: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            let style = rankStyle(room.myRank?.intValue)

            Text(room.myRank.map { "\($0)" } ?? "-")
                .font(.system(size: 14, weight: .medium))
                .kerning(-0.28)
                .foregroundColor(style.textColor)
                .frame(width: 26, height: 26)
                .background(style.background)
                .clipShape(Circle())
            VStack(alignment: .leading, spacing: 2) {
                Text(room.title)
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.textPrimary)
                Text("\(room.dateLabel) · \(room.questionCount)문항")
                    .font(.system(size: 12))
                    .kerning(-0.24)
                    .foregroundColor(PassmateColors.textSecondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if let score = room.myScore {
                Text("\(formatScore(Double(truncating: score)))점")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.textPrimary)
            }
            if room.hasReport {
                Button(action: onClickReport) {
                    Text("리포트")
                        .font(.system(size: 14, weight: .medium))
                        .kerning(-0.28)
                        .foregroundColor(PassmateColors.primaryDeep)
                        .padding(.horizontal, 12)
                        .frame(height: 30)
                        .background(PassmateColors.fieldGray)
                        .clipShape(Capsule())
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(PassmateColors.border, lineWidth: 1))
        .cornerRadius(16)
    }
}

private struct MyInfoNoticeToast: View {
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

private func rankStyle(_ rank: Int?) -> (background: Color, textColor: Color) {
    if rank == 1 {
        return (PassmateColors.chipGold, PassmateColors.chipGoldText)
    } else if rank == 2 {
        return (PassmateColors.chipBlue, PassmateColors.chipBlueText)
    } else if rank == 3 {
        return (PassmateColors.chipOrange, PassmateColors.chipOrangeText)
    } else {
        return (PassmateColors.fieldGray, PassmateColors.textSecondary)
    }
}

private func formatPin(_ pin: String) -> String {
    stride(from: 0, to: pin.count, by: 3)
        .map { start in
            let s = pin.index(pin.startIndex, offsetBy: start)
            let e = pin.index(s, offsetBy: 3, limitedBy: pin.endIndex) ?? pin.endIndex

            return String(pin[s..<e])
        }
        .joined(separator: " ")
}

private func formatRank(_ rank: Double) -> String {
    let rounded = Int((rank * 10).rounded())

    if rounded % 10 == 0 {
        return "\(rounded / 10)"
    } else {
        return "\(rounded / 10).\(rounded % 10)"
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

// 칩 가로 흐름 배치 (iOS 16+)
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
