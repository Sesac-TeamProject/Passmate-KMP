import SwiftUI
import Shared

// Figma "UI 디자인 v6" M-T4(349:10199) 미러 — 정산: 이번 달 수익(80%)·다음 지급·결제/정산 내역+계좌 관리 시트
struct EarningsView: View {
    var onRequireSignIn: () -> Void = {}

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
            .presentationDetents([.medium, .large])
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
                errorView
            } else if let earnings = uiState.earnings {
                loadedView(earnings)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(PassmateColors.surface.ignoresSafeArea())
    }

    private var errorView: some View {
        VStack(spacing: 12) {
            Text("정산 정보를 불러오지 못했어요")
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
                    Text("아직 정산 내역이 없어요 · 유료 방을 열면 여기에 쌓여요")
                        .font(.system(size: 14))
                        .kerning(-0.28)
                        .foregroundColor(PassmateColors.textSecondary)
                        .padding(.vertical, 24)
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
