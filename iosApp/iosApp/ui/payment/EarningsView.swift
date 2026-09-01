import SwiftUI
import Shared

// Figma "UI 디자인 v6" M-T4(349:10199) 미러 — 정산: 이번 달 수익(80%)·다음 지급·결제/정산 내역+계좌 관리 시트
struct EarningsView: View {
    var onRequireSignIn: () -> Void = {}

    // 빈 상태 「유료 방 만들기」 CTA — 방 개설 진입점인 「내가 만든 방」 탭으로 보낸다
    var onOpenHostedRooms: () -> Void = {}

    var onBack: () -> Void = {}

    @StateObject private var viewModel = EarningsViewModel(
        getEarningsUseCase: KoinHelper.shared.getEarningsUseCase(),
        isSignedInUseCase: KoinHelper.shared.isSignedInUseCase()
    )

    @State private var isAccountSheetVisible = false

    @State private var noticeMessage: String?

    var body: some View {
        EarningsContentView(
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
            case .openAccountSheet:
                isAccountSheetVisible = true
            case .openHostedRooms:
                onOpenHostedRooms()
            case let .showNotice(message):
                noticeMessage = message
            }
        }
        .sheet(isPresented: $isAccountSheetVisible) {
            SettlementAccountSheetView(
                onSaved: {
                    isAccountSheetVisible = false
                    viewModel.action(.accountSaved)
                },
                onNotice: { message in
                    viewModel.action(.notice(message: message))
                },
                onClose: { isAccountSheetVisible = false }
            )
            .passmateDetents([.medium, .large])
        }
        .overlay(alignment: .bottom) {
            if let noticeMessage {
                EarningsNoticeToast(message: noticeMessage)
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                            self.noticeMessage = nil
                        }
                    }
            }
        }
    }
}

private struct EarningsNoticeToast: View {
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

private struct EarningsContentView: View {
    let uiState: EarningsUiState

    let onAction: (EarningsAction) -> Void

    let onClickBack: () -> Void

    var body: some View {
        Group {
            if uiState.isLoading {
                ProgressView()
                    .tint(PassmateColors.primary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if uiState.loadFailed || uiState.earnings == nil {
                loadFailedView
            } else if let earnings = uiState.earnings {
                loadedView(earnings)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(PassmateColors.surface.ignoresSafeArea())
    }

    // 목록 불러오기 실패 (v6 E-List 공통 패턴) — Compose EarningsScreen.kt의 LoadFailedContent 미러.
    // TODO 공통화 대상: 코인 내역·참여한 방·마이가 같은 패턴을 쓴다. 화면별 적용이 끝나면 공통 컴포넌트로 승격한다
    private var loadFailedView: some View {
        VStack(spacing: 0) {
            loadFailedHeader
            VStack(spacing: 0) {
                ZStack {
                    Circle().fill(PassmateColors.errorIconBg)
                    AlertCircleIcon(tint: PassmateColors.wrongPinkText, size: 30)
                }
                .frame(width: 64, height: 64)
                Text("목록을 불러오지 못했어요")
                    .font(.system(size: 19, weight: .bold))
                    .kerning(-0.19)
                    .foregroundColor(PassmateColors.textPrimary)
                    .multilineTextAlignment(.center)
                    .padding(.top, 24)
                // lineSpacing 6.3 = Compose lineHeight 23 - 기본 행높이
                Text("연결이 잠시 끊겼어요.\n정산 금액은 사라지지 않아요.")
                    .font(.system(size: 14))
                    .kerning(-0.14)
                    .lineSpacing(6.3)
                    .foregroundColor(PassmateColors.textSecondary)
                    .multilineTextAlignment(.center)
                    .padding(.top, 8)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .padding(.horizontal, 20)
            Button {
                onAction(.retry)
            } label: {
                Text("다시 시도")
                    .font(.system(size: 15, weight: .bold))
                    .kerning(-0.15)
                    .foregroundColor(PassmateColors.surface)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(PassmateColors.primary)
                    .cornerRadius(14)
            }
            .padding(.horizontal, 20)
            // 정산은 마이 탭에서 push된 화면이라 뒤로가기가 곧 마이다
            Button(action: onClickBack) {
                Text("계좌 정보는 마이에서 확인")
                    .font(.system(size: 13, weight: .medium))
                    .kerning(-0.13)
                    .foregroundColor(PassmateColors.primaryDeep)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
            }
            .padding(.top, 8)
            .padding(.bottom, 24)
        }
    }

    private var loadFailedHeader: some View {
        HStack(spacing: 12) {
            Button(action: onClickBack) {
                Text("←")
                    .font(.system(size: 20))
                    .foregroundColor(PassmateColors.textPrimary)
            }
            Text("정산")
                .font(.system(size: 15, weight: .bold))
                .kerning(-0.15)
                .foregroundColor(PassmateColors.textPrimary)
            Spacer()
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 14)
    }

    private func loadedView(_ earnings: Earnings) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                HStack {
                    Button(action: onClickBack) {
                        Text("←")
                            .font(.system(size: 20))
                            .foregroundColor(PassmateColors.textPrimary)
                    }
                    Text("정산")
                        .font(.system(size: 24, weight: .bold))
                        .kerning(-0.48)
                        .foregroundColor(PassmateColors.textPrimary)
                        .padding(.leading, 8)
                    Spacer()
                    Button(action: { onAction(.clickManageAccount) }) {
                        Text("계좌 관리")
                            .font(.system(size: 14, weight: .medium))
                            .kerning(-0.28)
                            .foregroundColor(PassmateColors.primaryDeep)
                    }
                }
                summaryCard(earnings)
                Text("결제 · 정산 내역")
                    .font(.system(size: 18, weight: .bold))
                    .kerning(-0.36)
                    .foregroundColor(PassmateColors.textPrimary)
                if uiState.items.isEmpty {
                    // 빈 상태는 두 갈래다 — 계좌가 없으면 계좌 등록이 먼저다(정산 금액이 쌓여도 지급되지 않는다).
                    // 계좌가 있으면 "정산 내역이 없어요" + 유료 방 개설 유도 (v6 M-T4 빈 상태 2종)
                    if earnings.account == nil {
                        EmptyStateBlockView(
                            title: "정산 계좌를 등록해 주세요",
                            description: "계좌가 없으면 정산 금액이 쌓여도\n지급되지 않아요.",
                            buttonLabel: "계좌 등록하기",
                            onClickButton: { onAction(.clickManageAccount) }
                        ) {
                            AlertCircleIcon(tint: PassmateColors.wrongPinkText, size: 28)
                        }
                    } else {
                        EmptyStateBlockView(
                            title: "아직 정산 내역이 없어요",
                            description: "유료 방을 열고 참가비가 모이면\n매월 5일에 정산해 드려요.",
                            buttonLabel: "유료 방 만들기",
                            onClickButton: { onAction(.clickCreatePaidRoom) }
                        ) {
                            BookmarkIcon(tint: PassmateColors.primaryDeep, size: 28)
                        }
                    }
                }
                ForEach(uiState.items, id: \.settlementId) { item in
                    SettlementRowView(item: item)
                }
                if uiState.nextCursor != nil {
                    loadMoreButton
                }
                if let account = earnings.account {
                    accountRow(account)
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 32)
            .padding(.bottom, 24)
        }
    }

    private func summaryCard(_ earnings: Earnings) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("이번 달 수익 (선생님 \(earnings.hostSharePercent)%)")
                .font(.system(size: 13, weight: .medium))
                .kerning(-0.26)
                .foregroundColor(PassmateColors.primaryDeep)
            Text("₩ \(formatAmount(earnings.monthlyTotal))")
                .font(.system(size: 30, weight: .bold))
                .kerning(-0.6)
                .foregroundColor(PassmateColors.primaryDeep)
            Text(summaryLine(earnings))
                .font(.system(size: 13))
                .kerning(-0.26)
                .foregroundColor(PassmateColors.textSecondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(18)
        .background(PassmateColors.backgroundMint)
        .cornerRadius(20)
    }

    private func accountRow(_ account: SettlementAccountSummary) -> some View {
        Button(action: { onAction(.clickManageAccount) }) {
            HStack(spacing: 10) {
                Text(String(account.bankName.prefix(1)))
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(PassmateColors.primaryDeep)
                    .frame(width: 32, height: 32)
                    .background(PassmateColors.backgroundMint)
                    .cornerRadius(10)
                VStack(alignment: .leading, spacing: 2) {
                    Text("\(account.bankName) \(account.maskedNumber)")
                        .font(.system(size: 14, weight: .medium))
                        .kerning(-0.28)
                        .foregroundColor(PassmateColors.textPrimary)
                    if let note = account.payoutNote {
                        Text(note)
                            .font(.system(size: 12))
                            .kerning(-0.24)
                            .foregroundColor(PassmateColors.textTertiary)
                    }
                }
                Spacer()
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 14)
            .background(PassmateColors.surface)
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(PassmateColors.border, lineWidth: 1))
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
                    Text("전체 보기")
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

    private func summaryLine(_ earnings: Earnings) -> String {
        var parts: [String] = []

        if let nextPayout = earnings.nextPayout {
            parts.append("다음 지급 \(nextPayout.dateLabel) · ₩\(formatAmount(nextPayout.amount)) 예정")
        }
        parts.append("유료 방 \(earnings.paidRoomCount)회")
        parts.append("\(earnings.studentCount)명")

        return parts.joined(separator: " · ")
    }

    private func formatAmount(_ amount: Int64) -> String {
        let formatter = NumberFormatter()

        formatter.numberStyle = .decimal
        return formatter.string(from: NSNumber(value: amount)) ?? "\(amount)"
    }
}

private struct SettlementRowView: View {
    let item: SettlementItem

    var body: some View {
        HStack(spacing: 10) {
            Text(item.dateLabel)
                .font(.system(size: 12, weight: .bold))
                .foregroundColor(PassmateColors.primaryDeep)
                .padding(.horizontal, 8)
                .padding(.vertical, 10)
                .background(PassmateColors.backgroundMint)
                .cornerRadius(10)
            VStack(alignment: .leading, spacing: 3) {
                Text(item.roomTitle)
                    .font(.system(size: 14, weight: .bold))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.textPrimary)
                Text("\(item.participantCount)명 · 참가비 ₩\(formatAmount(item.entryFeeTotal)) · 수수료 ₩\(formatAmount(item.feeAmount))")
                    .font(.system(size: 12))
                    .kerning(-0.24)
                    .foregroundColor(PassmateColors.textSecondary)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 4) {
                Text("₩\(formatAmount(item.payoutAmount))")
                    .font(.system(size: 14, weight: .bold))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.textPrimary)
                statusChip
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 14)
        .background(PassmateColors.surface)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(PassmateColors.border, lineWidth: 1))
    }

    private var statusChip: some View {
        let (label, bg, fg) = statusStyle

        return Text(label)
            .font(.system(size: 12, weight: .medium))
            .foregroundColor(fg)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(bg)
            .cornerRadius(8)
    }

    private var statusStyle: (String, Color, Color) {
        if item.status == SettlementStatus.scheduled {
            return ("정산 예정", PassmateColors.chipGold, PassmateColors.chipGoldText)
        } else if item.status == SettlementStatus.paid {
            return ("지급 완료", PassmateColors.chipGreen, PassmateColors.chipGreenText)
        } else if item.status == SettlementStatus.held {
            return ("보류", PassmateColors.wrongPink, PassmateColors.wrongPinkText)
        } else {
            return ("확인 중", PassmateColors.fieldGray, PassmateColors.textSecondary)
        }
    }

    private func formatAmount(_ amount: Int64) -> String {
        let formatter = NumberFormatter()

        formatter.numberStyle = .decimal
        return formatter.string(from: NSNumber(value: amount)) ?? "\(amount)"
    }
}

// 빈 상태 블록 (v6 M-T4 빈 상태 2종) — 아이콘 원형 64 · 제목 19/Bold · 안내 2줄 · CTA 200x52.
// Compose EarningsScreen.kt의 EmptyStateBlock 미러
private struct EmptyStateBlockView<Icon: View>: View {
    let title: String

    let description: String

    let buttonLabel: String

    let onClickButton: () -> Void

    let icon: Icon

    var body: some View {
        VStack(spacing: 0) {
            ZStack {
                Circle().fill(PassmateColors.emptyIconBg)
                icon
            }
            .frame(width: 64, height: 64)
            Text(title)
                .font(.system(size: 19, weight: .bold))
                .kerning(-0.19)
                .foregroundColor(PassmateColors.textPrimary)
                .multilineTextAlignment(.center)
                .padding(.top, 24)
            // lineSpacing 6.3 = Compose lineHeight 23 - 기본 행높이
            Text(description)
                .font(.system(size: 14))
                .kerning(-0.14)
                .lineSpacing(6.3)
                .foregroundColor(PassmateColors.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.top, 8)
            Button(action: onClickButton) {
                Text(buttonLabel)
                    .font(.system(size: 15, weight: .bold))
                    .kerning(-0.15)
                    .foregroundColor(PassmateColors.surface)
                    .frame(width: 200, height: 52)
                    .background(PassmateColors.primary)
                    .cornerRadius(14)
            }
            .padding(.top, 20)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 40)
    }

    init(
        title: String,
        description: String,
        buttonLabel: String,
        onClickButton: @escaping () -> Void,
        @ViewBuilder icon: () -> Icon
    ) {
        self.title = title
        self.description = description
        self.buttonLabel = buttonLabel
        self.onClickButton = onClickButton
        self.icon = icon()
    }
}

// alert-circle — 원형 외곽선 + 느낌표. 아이콘 에셋이 없어 기본 도형으로 구성한다 (Compose AlertCircleIcon 미러)
private struct AlertCircleIcon: View {
    let tint: Color

    let size: CGFloat

    var body: some View {
        ZStack {
            Circle().strokeBorder(tint, lineWidth: 2)
            Text("!")
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(tint)
        }
        .frame(width: size, height: size)
    }
}

// bookmark 아이콘 — 아이콘 에셋이 없어 시안 벡터(24 뷰포트)를 직접 그린다.
// Compose EarningsScreen.kt의 bookmarkVector와 좌표가 1:1이다
private struct BookmarkIcon: View {
    let tint: Color

    let size: CGFloat

    private var scale: CGFloat {
        return size / 24
    }

    private var strokeStyle: StrokeStyle {
        return StrokeStyle(lineWidth: 2 * scale, lineCap: .round, lineJoin: .round)
    }

    private func point(_ x: CGFloat, _ y: CGFloat) -> CGPoint {
        return CGPoint(x: x * scale, y: y * scale)
    }

    private var ribbon: Path {
        var path = Path()

        path.move(to: point(6, 3))
        path.addLine(to: point(18, 3))
        path.addQuadCurve(to: point(19, 4), control: point(19, 3))
        path.addLine(to: point(19, 21))
        path.addLine(to: point(12, 17))
        path.addLine(to: point(5, 21))
        path.addLine(to: point(5, 4))
        path.addQuadCurve(to: point(6, 3), control: point(5, 3))
        path.closeSubpath()

        return path
    }

    private var innerLines: Path {
        var path = Path()

        path.move(to: point(9, 8))
        path.addLine(to: point(15, 8))
        path.move(to: point(9, 12))
        path.addLine(to: point(15, 12))

        return path
    }

    var body: some View {
        ZStack(alignment: .topLeading) {
            ribbon.stroke(tint, style: strokeStyle)
            innerLines.stroke(tint, style: strokeStyle)
        }
        .frame(width: size, height: size)
    }
}
