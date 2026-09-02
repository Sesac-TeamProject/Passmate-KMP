import SwiftUI
import Shared

// Figma "UI 디자인 v6" M-08(349:9544) 미러 — 진행 중 방·누적 요약·보완 주제·참여한 방 목록(→리포트) (T064)
struct JoinedRoomsView: View {
    var onRequireSignIn: () -> Void = {}

    var onOpenReport: (Int64) -> Void = { _ in }

    var onRejoin: (String) -> Void = { _ in }

    // 홈 탭이 곧 PIN 입장 폼 (규칙 §2-1-1) — Compose는 NavigationAction.NavigateToHome
    var onOpenPinEntry: () -> Void = {}

    @StateObject private var viewModel = JoinedRoomsViewModel(
        getMyPageUseCase: KoinHelper.shared.getMyPageUseCase(),
        isSignedInUseCase: KoinHelper.shared.isSignedInUseCase()
    )

    @State private var noticeMessage: String?

    var body: some View {
        JoinedRoomsContentView(
            uiState: viewModel.uiState,
            onAction: { viewModel.action($0) }
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
            case .openPinEntry:
                onOpenPinEntry()
            case let .showNotice(message):
                noticeMessage = message
            }
        }
        .overlay(alignment: .bottom) {
            if let noticeMessage {
                JoinedRoomsNoticeToast(message: noticeMessage)
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                            self.noticeMessage = nil
                        }
                    }
            }
        }
    }
}

private struct JoinedRoomsContentView: View {
    let uiState: JoinedRoomsUiState

    let onAction: (JoinedRoomsAction) -> Void

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
                Text("참여한 방")
                    .font(.system(size: 24, weight: .bold))
                    .kerning(-0.48)
                    .foregroundColor(PassmateColors.textPrimary)
                if let ongoing = uiState.ongoing {
                    OngoingCard(ongoing: ongoing, onClickRejoin: { onAction(.clickRejoin(pin: ongoing.pin)) })
                }
                if let summary = uiState.summary {
                    SummaryCard(summary: summary)
                    WeakTopicsRow(topics: summary.weakTopics)
                }
                if uiState.rooms.isEmpty, uiState.ongoing == nil {
                    JoinedRoomsEmptyView(onClickEnterPin: { onAction(.clickEnterPin) })
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
            .padding(.bottom, 96)
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

// 빈 상태 문구 (v6 M-08) — Compose JoinedRoomsScreen.kt의 EmptyStateText 미러
private enum EmptyStateText {
    static let title = "아직 참여한 방이 없어요"

    static let guide = "선생님에게 받은 PIN 6자리를\n홈에서 입력해 보세요."

    static let cta = "PIN으로 입장"
}

// 빈 상태 치수·타이포 (v6 M-08) — Compose EmptyStateSpec 미러
private enum EmptyStateSpec {
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

// 빈 상태 (v6 M-08) 미러 — 아이콘 원형 · 제목 · 안내 문구 · PIN 입장 CTA. 값은 EmptyStateSpec/EmptyStateText
private struct JoinedRoomsEmptyView: View {
    let onClickEnterPin: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            ZStack {
                Circle()
                    .fill(PassmateColors.emptyIconBg)
                PassmateIconView(icon: .doorOpen, tint: PassmateColors.textSecondary, size: EmptyStateSpec.iconSize)
            }
            .frame(width: EmptyStateSpec.iconCircleSize, height: EmptyStateSpec.iconCircleSize)
            Text(EmptyStateText.title)
                .font(.system(size: EmptyStateSpec.titleFontSize, weight: .bold))
                .kerning(EmptyStateSpec.titleKerning)
                .foregroundColor(PassmateColors.textPrimary)
                .multilineTextAlignment(.center)
                .padding(.top, EmptyStateSpec.titleTopPadding)
            Text(EmptyStateText.guide)
                .font(.system(size: EmptyStateSpec.guideFontSize))
                .lineSpacing(EmptyStateSpec.guideLineSpacing)
                .foregroundColor(PassmateColors.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.top, EmptyStateSpec.guideTopPadding)
            Button(action: onClickEnterPin) {
                Text(EmptyStateText.cta)
                    .font(.system(size: EmptyStateSpec.ctaFontSize, weight: .bold))
                    .foregroundColor(PassmateColors.surface)
                    .frame(width: EmptyStateSpec.ctaWidth, height: EmptyStateSpec.ctaHeight)
                    .background(PassmateColors.primary)
                    .cornerRadius(EmptyStateSpec.ctaCornerRadius)
            }
            .padding(.top, EmptyStateSpec.ctaTopPadding)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, EmptyStateSpec.sectionPaddingVertical)
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

private struct JoinedRoomsNoticeToast: View {
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

// MARK: - 프리뷰 (Figma 시안 비교용, 백엔드 불필요)

#Preview("진행 중 방 + 참여한 방 3개") {
    JoinedRoomsContentView(
        uiState: JoinedRoomsUiState(
            isLoading: false,
            summary: MyPageSummary(
                participationCount: 12,
                accuracyPercent: 78,
                avgRank: KotlinDouble(double: 2.4),
                trendText: "지난주보다 +5%",
                weakTopics: ["이차함수", "확률과 통계"]
            ),
            ongoing: OngoingRoom(
                roomId: 501,
                pin: "482913",
                title: "8월 4주차 Spring 스터디",
                hostNickname: "김선생",
                progressLabel: "5 / 8 문항 진행 중"
            ),
            rooms: [
                JoinedRoom(roomId: 401, title: "7월 3주차 미적분 특강", dateLabel: "2026.07.18", questionCount: 10, myScore: KotlinDouble(double: 890), myRank: KotlinInt(int: 2), hasReport: true),
                JoinedRoom(roomId: 402, title: "확률과 통계 총정리", dateLabel: "2026.07.10", questionCount: 8, myScore: KotlinDouble(double: 720), myRank: KotlinInt(int: 5), hasReport: true),
                JoinedRoom(roomId: 403, title: "함수의 극한 퀴즈", dateLabel: "2026.06.28", questionCount: 6, myScore: nil, myRank: nil, hasReport: false)
            ]
        ),
        onAction: { _ in }
    )
}

#Preview("참여한 방 없음") {
    JoinedRoomsContentView(
        uiState: JoinedRoomsUiState(isLoading: false, rooms: []),
        onAction: { _ in }
    )
}
