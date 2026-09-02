import SwiftUI
import Shared

// 코인 내역 (M-12-9) — Compose CoinHistoryScreen.kt 미러.
// 빈 상태는 "빈 상태 — 코인 내역", 목록 실패는 "E-List 목록 불러오기 실패 — 공통 패턴"을 따른다
struct CoinHistoryView: View {
    @StateObject private var viewModel = CoinHistoryViewModel(
        getMyCoinsUseCase: KoinHelper.shared.getMyCoinsUseCase(),
        getCoinTransactionsUseCase: KoinHelper.shared.getCoinTransactionsUseCase()
    )

    var onBack: () -> Void = {}

    var onOpenCoinCharge: () -> Void = {}

    @State private var noticeMessage: String?

    var body: some View {
        CoinHistoryContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.action,
            onBack: onBack
        )
        .onAppear { viewModel.action(.enter) }
        .onReceive(viewModel.event) { event in
            switch event {
            case .openCoinCharge:
                onOpenCoinCharge()
            case let .showNotice(message):
                noticeMessage = message
            }
        }
        .overlay(alignment: .bottom) {
            if let noticeMessage {
                Text(noticeMessage)
                    .font(.system(size: 13))
                    .foregroundColor(PassmateColors.surface)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(PassmateColors.textPrimary.opacity(0.9))
                    .cornerRadius(10)
                    .padding(.bottom, 16)
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                            self.noticeMessage = nil
                        }
                    }
            }
        }
    }
}

private struct CoinHistoryContentView: View {
    let uiState: CoinHistoryUiState

    let onAction: (CoinHistoryAction) -> Void

    let onBack: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            topBar
            content
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(PassmateColors.surface.ignoresSafeArea())
    }

    private var topBar: some View {
        HStack(spacing: 12) {
            Button(action: onBack) {
                PassmateIconView(icon: .arrowLeft, tint: PassmateColors.textPrimary, size: 22)
            }
            .accessibilityLabel("뒤로 가기")
            Text("코인 내역")
                .font(.system(size: 20, weight: .bold))
                .kerning(-0.4)
                .foregroundColor(PassmateColors.textPrimary)
        }
        .padding(.top, 16)
        .padding(.horizontal, 20)
        .padding(.bottom, 16)
    }

    @ViewBuilder
    private var content: some View {
        if uiState.isLoading {
            centerProgress
        } else if uiState.hasError {
            loadFailureBody
        } else if uiState.isEmpty {
            emptyBody
        } else {
            historyBody
        }
    }

    private var historyBody: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                balanceCard.padding(.top, 8)
                filterChipRow.padding(.top, 16)
                transactionCard.padding(.top, 16)
                if uiState.hasNext {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                        .padding(12)
                        .onAppear { onAction(.loadMore) }
                }
                Spacer().frame(height: 24)
            }
            .padding(.horizontal, 20)
        }
    }

    // 시안 card/잔액 — 민트 배경 + 코인 마크 + 우측 잔액. 잔액 조회 실패 시 "-"로 그린다
    private var balanceCard: some View {
        HStack(spacing: 10) {
            coinMark
            Text("보유 코인")
                .font(.system(size: 14))
                .foregroundColor(PassmateColors.textSecondary)
            Spacer()
            Text(balanceText)
                .font(.system(size: 20, weight: .bold))
                .kerning(-0.4)
                .foregroundColor(PassmateColors.primaryDeep)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity, minHeight: 128)
        .background(PassmateColors.backgroundMint)
        .cornerRadius(16)
    }

    private var coinMark: some View {
        PassmateIconView(icon: .coin, tint: PassmateColors.textPrimary, size: 24)
    }

    private var filterChipRow: some View {
        HStack(spacing: 8) {
            ForEach(CoinHistoryFilter.allCases, id: \.self) { filter in
                filterChip(filter)
            }
        }
    }

    private func filterChip(_ filter: CoinHistoryFilter) -> some View {
        let isSelected = filter == uiState.filter
        let background = isSelected ? PassmateColors.filterChipSelectedBg : PassmateColors.fieldGray
        let textColor = isSelected ? PassmateColors.textPrimary : PassmateColors.textSecondary

        return Button(action: { onAction(.selectFilter(filter: filter)) }) {
            Text(filter.label)
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(textColor)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(background)
                .clipShape(Capsule())
        }
    }

    // 시안 card — 흰 카드 1장 안에 행을 쌓고 마지막 행만 구분선을 뺀다
    private var transactionCard: some View {
        VStack(spacing: 0) {
            ForEach(Array(uiState.visibleItems.enumerated()), id: \.offset) { index, transaction in
                TransactionRowView(transaction: transaction)
                if index != uiState.visibleItems.count - 1 {
                    PassmateColors.border.frame(height: 1)
                }
            }
        }
        .frame(maxWidth: .infinity)
        .background(PassmateColors.surface)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(PassmateColors.border, lineWidth: 1))
        .cornerRadius(16)
    }

    // 빈 상태 — 코인 내역 (M-12-9)
    private var emptyBody: some View {
        VStack(spacing: 0) {
            Spacer()
            emptyIcon
            Text("아직 코인 내역이 없어요")
                .font(.system(size: 19, weight: .bold))
                .foregroundColor(PassmateColors.textPrimary)
                .padding(.top, 24)
            Text("코인을 충전하거나 유료 방에 참여하면\n여기에 기록이 남아요.")
                .font(.system(size: 14))
                .lineSpacing(23 - 14)
                .multilineTextAlignment(.center)
                .foregroundColor(PassmateColors.textSecondary)
                .padding(.top, 8)
            Button(action: { onAction(.clickCharge) }) {
                Text("코인 충전하기")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(PassmateColors.surface)
                    .frame(width: 200, height: 52)
                    .background(PassmateColors.primary)
                    .cornerRadius(14)
            }
            .padding(.top, 20)
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(.horizontal, 20)
    }

    // E-List 목록 불러오기 실패 — 공통 패턴 (M-12-9 문구: 둘째 줄 "충전 기록은 사라지지 않아요.")
    // 다른 화면(참여한 방·마이·정산)에도 같은 패턴이 쓰이므로 추후 공통 컴포넌트로 승격 대상이다
    private var loadFailureBody: some View {
        VStack(spacing: 0) {
            Spacer()
            errorIcon
            Text("목록을 불러오지 못했어요")
                .font(.system(size: 19, weight: .bold))
                .foregroundColor(PassmateColors.textPrimary)
                .padding(.top, 24)
            Text("연결이 잠시 끊겼어요.\n충전 기록은 사라지지 않아요.")
                .font(.system(size: 14))
                .lineSpacing(23 - 14)
                .multilineTextAlignment(.center)
                .foregroundColor(PassmateColors.textSecondary)
                .padding(.top, 9)
            Spacer()
            Button(action: { onAction(.retry) }) {
                Text("다시 시도")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(PassmateColors.surface)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(PassmateColors.primary)
                    .cornerRadius(14)
            }
            Button(action: onBack) {
                Text("보유 코인은 마이에서 확인")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(PassmateColors.primaryDeep)
                    .padding(.vertical, 4)
                    .padding(.horizontal, 8)
            }
            .padding(.top, 18)
            Spacer().frame(height: 24)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(.horizontal, 20)
    }

    private var emptyIcon: some View {
        PassmateIconView(icon: .list, tint: PassmateColors.primaryDeep, size: 28)
            .frame(width: 64, height: 64)
            .background(PassmateColors.emptyIconBg)
            .clipShape(Circle())
    }

    private var errorIcon: some View {
        PassmateIconView(icon: .alertCircle, tint: PassmateColors.errorIconTint, size: 30)
            .frame(width: 64, height: 64)
            .background(PassmateColors.errorIconBg)
            .clipShape(Circle())
    }

    private var centerProgress: some View {
        VStack { Spacer(); ProgressView().tint(PassmateColors.primary); Spacer() }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var balanceText: String {
        if let balance = uiState.balance {
            return "\(CoinHistoryFormat.number(balance)) C"
        } else {
            return "- C"
        }
    }
}

private struct TransactionRowView: View {
    let transaction: CoinTransaction

    var body: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(PassmateColors.textPrimary)
                Text(shortDate)
                    .font(.system(size: 12))
                    .foregroundColor(PassmateColors.textSecondary)
            }
            Spacer()
            Text(amountText)
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(amountColor)
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
    }

    // 계약에 내역 설명 필드가 없어 type·roomTitle·method로 조합한다 (시안 "카카오페이 충전"·"… 참가비")
    private var title: String {
        switch transaction.type {
        case .charge:
            return transaction.method.map { "\($0.label) 충전" } ?? "코인 충전"
        case .deduct:
            return transaction.roomTitle.map { "\($0) 참가비" } ?? "참가비"
        case .refund:
            return transaction.roomTitle.map { "\($0) 환급" } ?? "코인 환급"
        default:
            return "코인 내역"
        }
    }

    // "2026-08-22T10:00:00Z" → "8/22" (시안은 월만 앞 0을 떼고 일은 두 자리 유지). 파싱 실패 시 원문 앞 10자
    private var shortDate: String {
        guard let raw = transaction.createdAt else { return "" }
        let head = String(raw.prefix(10))
        let parts = head.split(separator: "-")

        if parts.count == 3 {
            let month = String(parts[1]).drop { $0 == "0" }
            return "\(month)/\(parts[2])"
        } else {
            return head
        }
    }

    private var amountText: String {
        let amount = Int(transaction.amount)
        let sign = amount >= 0 ? "+" : "-"

        return "\(sign)\(CoinHistoryFormat.number(amount >= 0 ? amount : -amount)) C"
    }

    private var amountColor: Color {
        Int(transaction.amount) >= 0 ? PassmateColors.primaryDeep : PassmateColors.textPrimary
    }
}

private enum CoinHistoryFormat {
    static func number(_ value: Int) -> String {
        let digits = Array(String(value).reversed())
        var chunks: [String] = []

        for index in stride(from: 0, to: digits.count, by: 3) {
            let end = min(index + 3, digits.count)
            chunks.append(String(digits[index..<end]))
        }
        return String(chunks.joined(separator: ",").reversed())
    }
}

// MARK: - 프리뷰 (Figma 시안 비교용, 백엔드 불필요)

#Preview("M-12-9 코인 내역") {
    CoinHistoryContentView(
        uiState: CoinHistoryUiState(
            isLoading: false,
            balance: 1200,
            items: [
                CoinTransaction(id: 9001, type: .deduct, amount: -10000, balanceAfter: 1200, method: nil, roomTitle: "Spring 실전 모의고사", paymentNo: nil, createdAt: "2026-08-22T10:00:00Z"),
                CoinTransaction(id: 9002, type: .charge, amount: 10000, balanceAfter: 11200, method: .kakaoPay, roomTitle: nil, paymentNo: "PAY-1", createdAt: "2026-08-20T10:00:00Z"),
                CoinTransaction(id: 9003, type: .deduct, amount: -5000, balanceAfter: 1200, method: nil, roomTitle: "CS 기술면접 라운드 2", paymentNo: nil, createdAt: "2026-08-15T10:00:00Z"),
                CoinTransaction(id: 9004, type: .charge, amount: 5000, balanceAfter: 6200, method: .kakaoPay, roomTitle: nil, paymentNo: "PAY-2", createdAt: "2026-08-10T10:00:00Z"),
                CoinTransaction(id: 9005, type: .refund, amount: 1200, balanceAfter: 1200, method: nil, roomTitle: nil, paymentNo: nil, createdAt: "2026-08-01T10:00:00Z")
            ]
        ),
        onAction: { _ in },
        onBack: {}
    )
}

#Preview("빈 상태") {
    CoinHistoryContentView(
        uiState: CoinHistoryUiState(isLoading: false, balance: 0, items: []),
        onAction: { _ in },
        onBack: {}
    )
}

#Preview("목록 불러오기 실패") {
    CoinHistoryContentView(
        uiState: CoinHistoryUiState(isLoading: false, hasError: true),
        onAction: { _ in },
        onBack: {}
    )
}
