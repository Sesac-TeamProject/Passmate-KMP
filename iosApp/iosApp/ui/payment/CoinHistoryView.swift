import SwiftUI
import Shared

// 코인 사용·충전 내역 (M-12) — Compose CoinHistoryScreen.kt 미러
struct CoinHistoryView: View {
    @StateObject private var viewModel = CoinHistoryViewModel(
        getCoinTransactionsUseCase: KoinHelper.shared.getCoinTransactionsUseCase()
    )

    var onBack: () -> Void = {}

    var body: some View {
        CoinHistoryContentView(uiState: viewModel.uiState, onAction: viewModel.action, onBack: onBack)
            .onAppear { viewModel.action(.enter) }
    }
}

private struct CoinHistoryContentView: View {
    let uiState: CoinHistoryUiState

    let onAction: (CoinHistoryAction) -> Void

    let onBack: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Button(action: onBack) {
                Text("‹ 뒤로").font(.system(size: 14)).foregroundColor(PassmateColors.textSecondary)
            }
            .padding(.top, 16)
            Text("코인·결제 내역")
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(PassmateColors.textPrimary)
                .padding(.top, 8)
            content.padding(.top, 16)
        }
        .padding(.horizontal, 20)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(PassmateColors.backgroundMint.ignoresSafeArea())
    }

    @ViewBuilder
    private var content: some View {
        if uiState.isLoading {
            centerProgress
        } else if uiState.hasError {
            retryState
        } else if uiState.isEmpty {
            centerText("아직 코인 내역이 없어요")
        } else {
            list
        }
    }

    private var list: some View {
        ScrollView {
            LazyVStack(spacing: 10) {
                ForEach(uiState.items, id: \.id) { tx in
                    TransactionRowView(tx: tx)
                }
                if uiState.hasNext {
                    ProgressView().padding(12).onAppear { onAction(.loadMore) }
                }
                Spacer().frame(height: 16)
            }
        }
    }

    private var centerProgress: some View {
        VStack { Spacer(); ProgressView().tint(PassmateColors.primary); Spacer() }
            .frame(maxWidth: .infinity)
    }

    private func centerText(_ text: String) -> some View {
        VStack { Spacer(); Text(text).font(.system(size: 14)).foregroundColor(PassmateColors.textTertiary); Spacer() }
            .frame(maxWidth: .infinity)
    }

    private var retryState: some View {
        VStack(spacing: 12) {
            Spacer()
            Text("내역을 불러오지 못했어요").font(.system(size: 14)).foregroundColor(PassmateColors.textSecondary)
            Button(action: { onAction(.retry) }) {
                Text("다시 시도")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(PassmateColors.surface)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 10)
                    .background(PassmateColors.primary)
                    .cornerRadius(12)
            }
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }
}

private struct TransactionRowView: View {
    let tx: CoinTransaction

    var body: some View {
        PassmateCard {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).font(.system(size: 14, weight: .medium)).foregroundColor(PassmateColors.textPrimary)
                    if let subtitle {
                        Text(subtitle).font(.system(size: 12)).foregroundColor(PassmateColors.textTertiary)
                    }
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text(amountText).font(.system(size: 15, weight: .bold)).foregroundColor(amountColor)
                    Text("잔액 \(Int(tx.balanceAfter)) C").font(.system(size: 11)).foregroundColor(PassmateColors.textTertiary)
                }
            }
            .padding(16)
        }
    }

    private var title: String {
        switch tx.type {
        case .charge:
            return "코인 충전"
        case .deduct:
            return tx.roomTitle.map { "참가비 · \($0)" } ?? "참가비 차감"
        case .refund:
            return tx.roomTitle.map { "환급 · \($0)" } ?? "참가비 환급"
        default:
            return "코인 내역"
        }
    }

    private var subtitle: String? {
        switch tx.type {
        case .charge:
            return tx.method?.label
        default:
            return tx.paymentNo
        }
    }

    private var amountText: String {
        let amount = Int(tx.amount)
        let sign = amount >= 0 ? "+" : ""

        return "\(sign)\(amount) C"
    }

    private var amountColor: Color {
        Int(tx.amount) >= 0 ? PassmateColors.inkGreen : PassmateColors.textPrimary
    }
}

// MARK: - 프리뷰 (Figma 시안 비교용, 백엔드 불필요)

#Preview("내역 4건") {
    CoinHistoryContentView(
        uiState: CoinHistoryUiState(
            isLoading: false,
            items: [
                CoinTransaction(id: 9001, type: .charge, amount: 1000, balanceAfter: 1000, method: .kakaoPay, roomTitle: nil, paymentNo: "PAY-20260810-01", createdAt: "2026.08.10"),
                CoinTransaction(id: 9002, type: .deduct, amount: -500, balanceAfter: 500, method: nil, roomTitle: "8월 4주차 Spring 스터디", paymentNo: nil, createdAt: "2026.08.14"),
                CoinTransaction(id: 9003, type: .refund, amount: 500, balanceAfter: 1000, method: nil, roomTitle: "확률과 통계 총정리", paymentNo: nil, createdAt: "2026.08.16"),
                CoinTransaction(id: 9004, type: .deduct, amount: -300, balanceAfter: 700, method: nil, roomTitle: "함수의 극한 퀴즈", paymentNo: nil, createdAt: "2026.08.20")
            ]
        ),
        onAction: { _ in },
        onBack: {}
    )
}

#Preview("내역 없음") {
    CoinHistoryContentView(
        uiState: CoinHistoryUiState(isLoading: false, items: []),
        onAction: { _ in },
        onBack: {}
    )
}
