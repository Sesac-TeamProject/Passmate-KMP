import SwiftUI
import Shared

// Figma "UI 디자인 v6" M-T4(349:10199) 미러 — 정산: 이번 달 수익(80%)·다음 지급·결제/정산 내역+계좌 관리 시트
struct EarningsView: View {
    var onRequireSignIn: () -> Void = {}

    var onOpenSettlementAccount: () -> Void = {}

    // 빈 상태 「유료 방 만들기」 CTA — 방 개설 진입점인 「내가 만든 방」 탭으로 보낸다
    var onOpenHostedRooms: () -> Void = {}

    var onOpenCoinHistory: () -> Void = {}

    var onBack: () -> Void = {}

    @StateObject private var viewModel = EarningsViewModel(
        getEarningsUseCase: KoinHelper.shared.getEarningsUseCase(),
        isSignedInUseCase: KoinHelper.shared.isSignedInUseCase()
    )


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
                onOpenSettlementAccount()
            case .openHostedRooms:
                onOpenHostedRooms()
            case .openCoinHistory:
                onOpenCoinHistory()
            case let .showNotice(message):
                noticeMessage = message
            }
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
                    PassmateIconView(
                        icon: LoadFailedText.icon,
                        tint: PassmateColors.wrongPinkText,
                        size: LoadFailedSpec.iconSize
                    )
                }
                .frame(width: LoadFailedSpec.iconCircleSize, height: LoadFailedSpec.iconCircleSize)
                Text(LoadFailedText.title)
                    .font(.system(size: LoadFailedSpec.titleFontSize, weight: .bold))
                    .kerning(LoadFailedSpec.titleKerning)
                    .foregroundColor(PassmateColors.textPrimary)
                    .multilineTextAlignment(.center)
                    .padding(.top, LoadFailedSpec.titleTopPadding)
                Text(LoadFailedText.guide)
                    .font(.system(size: LoadFailedSpec.guideFontSize))
                    .kerning(LoadFailedSpec.guideKerning)
                    .lineSpacing(LoadFailedSpec.guideLineSpacing)
                    .foregroundColor(PassmateColors.textSecondary)
                    .multilineTextAlignment(.center)
                    .padding(.top, LoadFailedSpec.guideTopPadding)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .padding(.horizontal, LoadFailedSpec.contentPaddingHorizontal)
            Button {
                onAction(.retry)
            } label: {
                Text(LoadFailedText.retry)
                    .font(.system(size: LoadFailedSpec.retryFontSize, weight: .bold))
                    .kerning(LoadFailedSpec.retryKerning)
                    .foregroundColor(PassmateColors.surface)
                    .frame(maxWidth: .infinity)
                    .frame(height: LoadFailedSpec.retryHeight)
                    .background(PassmateColors.primary)
                    .cornerRadius(LoadFailedSpec.retryCornerRadius)
            }
            .padding(.horizontal, LoadFailedSpec.contentPaddingHorizontal)
            Button(action: onClickBack) {
                Text(LoadFailedText.backLink)
                    .font(.system(size: LoadFailedSpec.backLinkFontSize, weight: .medium))
                    .kerning(LoadFailedSpec.backLinkKerning)
                    .foregroundColor(PassmateColors.primaryDeep)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, LoadFailedSpec.backLinkVerticalPadding)
            }
            .padding(.top, LoadFailedSpec.backLinkTopPadding)
            .padding(.bottom, LoadFailedSpec.bottomSpacing)
        }
    }

    private var loadFailedHeader: some View {
        HStack(spacing: LoadFailedSpec.backEndPadding) {
            Button(action: onClickBack) {
                Text(LoadFailedText.back)
                    .font(.system(size: LoadFailedSpec.backFontSize))
                    .foregroundColor(PassmateColors.textPrimary)
                    .padding(.vertical, LoadFailedSpec.backVerticalPadding)
            }
            Text(LoadFailedText.headerTitle)
                .font(.system(size: LoadFailedSpec.headerTitleFontSize, weight: .bold))
                .kerning(LoadFailedSpec.headerTitleKerning)
                .foregroundColor(PassmateColors.textPrimary)
            Spacer()
        }
        .padding(.horizontal, LoadFailedSpec.headerPaddingHorizontal)
        .padding(.vertical, LoadFailedSpec.headerPaddingVertical)
    }

    private func loadedView(_ earnings: Earnings) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                HStack(spacing: 12) {
                    PassmateBackButton(onClick: onClickBack)
                    Text("정산")
                        .font(.system(size: 24, weight: .bold))
                        .kerning(-0.48)
                        .foregroundColor(PassmateColors.textPrimary)
                    Spacer()
                    Button(action: { onAction(.clickManageAccount) }) {
                        Text("계좌 관리")
                            .font(.system(size: 14, weight: .medium))
                            .kerning(-0.28)
                            .foregroundColor(PassmateColors.primaryDeep)
                    }
                }
                summaryCard(earnings)
                historySectionHeader
                if uiState.items.isEmpty {
                    // 빈 상태는 두 갈래다 — 계좌가 없으면 계좌 등록이 먼저다(정산 금액이 쌓여도 지급되지 않는다).
                    // 계좌가 있으면 "정산 내역이 없어요" + 유료 방 개설 유도 (v6 M-T4 빈 상태 2종)
                    if earnings.account == nil {
                        PassmateEmptyStateView(
                            icon: EmptyStateText.accountIcon,
                            iconTint: PassmateColors.wrongPinkText,
                            title: EmptyStateText.accountTitle,
                            guide: EmptyStateText.accountGuide,
                            ctaLabel: EmptyStateText.accountCta,
                            onClickCta: { onAction(.clickManageAccount) }
                        )
                    } else {
                        PassmateEmptyStateView(
                            icon: EmptyStateText.settlementsIcon,
                            iconTint: PassmateColors.primaryDeep,
                            title: EmptyStateText.settlementsTitle,
                            guide: EmptyStateText.settlementsGuide,
                            ctaLabel: EmptyStateText.settlementsCta,
                            onClickCta: { onAction(.clickCreatePaidRoom) }
                        )
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

    // 시안 M-T4 — 섹션 제목과 "전체 보기 ›" 링크를 좌우 양끝 정렬한다
    private var historySectionHeader: some View {
        HStack {
            Text("결제 · 정산 내역")
                .font(.system(size: 18, weight: .bold))
                .kerning(-0.36)
                .foregroundColor(PassmateColors.textPrimary)
            Spacer()
            Button(action: { onAction(.clickViewAllHistory) }) {
                Text("전체 보기 ›")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.primaryDeep)
            }
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

// 빈 상태 문구 (v6 M-T4 2종) — Compose EarningsScreen.kt의 EmptyStateText 미러.
// 치수·타이포는 공통 컴포넌트 PassmateEmptyStateView가 갖는다
private enum EmptyStateText {
    static let settlementsTitle = "아직 정산 내역이 없어요"

    static let settlementsGuide = "유료 방을 열고 참가비가 모이면\n매월 5일에 정산해 드려요."

    static let settlementsCta = "유료 방 만들기"

    static let settlementsIcon = PassmateIcons.bookmark

    static let accountTitle = "정산 계좌를 등록해 주세요"

    static let accountGuide = "계좌가 없으면 정산 금액이 쌓여도\n지급되지 않아요."

    static let accountCta = "계좌 등록하기"

    static let accountIcon = PassmateIcons.alertCircle
}

// 목록 불러오기 실패 문구 (v6 E-List) — Compose EarningsScreen.kt의 LoadFailedText 미러
private enum LoadFailedText {
    static let headerTitle = "정산"

    static let back = "\u{2190}"

    static let title = "목록을 불러오지 못했어요"

    static let guide = "연결이 잠시 끊겼어요.\n정산 금액은 사라지지 않아요."

    static let retry = "다시 시도"

    // 정산은 마이 탭에서 push된 화면이라 뒤로가기가 곧 마이다
    static let backLink = "계좌 정보는 마이에서 확인"

    static let icon = PassmateIcons.alertCircle
}

// 목록 불러오기 실패 치수·타이포 (v6 E-List) — Compose LoadFailedSpec 미러.
// 상단 여백은 Compose가 60dp를 직접 두는 것과 달리 세이프에어리어가 처리하므로 항목이 없다.
// (SwiftUI에는 lineHeight가 없어 파생값 guideLineSpacing 1개가 더 있다)
private enum LoadFailedSpec {
    static let headerPaddingHorizontal: CGFloat = 20

    static let headerPaddingVertical: CGFloat = 14

    static let backFontSize: CGFloat = 20

    static let backEndPadding: CGFloat = 12

    // Compose BackVerticalPadding 4dp와 같은 탭 영역 확장
    static let backVerticalPadding: CGFloat = 4

    static let headerTitleFontSize: CGFloat = 15

    static let headerTitleKerning: CGFloat = -0.15

    static let contentPaddingHorizontal: CGFloat = 20

    static let iconCircleSize: CGFloat = 64

    static let iconSize: CGFloat = 30

    static let titleTopPadding: CGFloat = 24

    static let titleFontSize: CGFloat = 19

    static let titleKerning: CGFloat = -0.19

    static let guideTopPadding: CGFloat = 8

    static let guideFontSize: CGFloat = 14

    // Compose lineHeight 23.1(14 x 1.65) - SF 14pt 기본 행높이
    static let guideLineSpacing: CGFloat = 6.4

    static let guideKerning: CGFloat = -0.14

    static let retryHeight: CGFloat = 52

    static let retryCornerRadius: CGFloat = 14

    static let retryFontSize: CGFloat = 15

    static let retryKerning: CGFloat = -0.15

    static let backLinkTopPadding: CGFloat = 8

    static let backLinkVerticalPadding: CGFloat = 10

    static let backLinkFontSize: CGFloat = 13

    static let backLinkKerning: CGFloat = -0.13

    static let bottomSpacing: CGFloat = 24
}
