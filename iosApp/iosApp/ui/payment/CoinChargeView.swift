import SwiftUI
import Shared

// 코인 충전 (M-12-4 금액 선택 · M-12-6 완료) — Compose CoinChargeScreen.kt 미러.
// 결제창 UI는 포트원 SDK가 그리므로(시안 M-12-5는 그 목업) PortOnePaymentView만 덮어씌운다
struct CoinChargeView: View {
    @StateObject private var viewModel = CoinChargeViewModel(
        getMyCoinsUseCase: KoinHelper.shared.getMyCoinsUseCase(),
        requestChargeUseCase: KoinHelper.shared.requestChargeUseCase(),
        confirmChargeUseCase: KoinHelper.shared.confirmChargeUseCase(),
        coinPolicy: KoinHelper.shared.coinPolicy()
    )

    var onBack: () -> Void = {}

    @State private var noticeMessage: String?

    var body: some View {
        ZStack {
            CoinChargeContentView(
                uiState: viewModel.uiState,
                onAction: viewModel.action,
                onBack: onBack
            )
            if let request = viewModel.uiState.checkout {
                PortOnePaymentView(request: request) { result in
                    viewModel.action(.receivePortOneResult(result: result))
                }
                .background(PassmateColors.surface.ignoresSafeArea())
            }
        }
        .onAppear { viewModel.action(.enter) }
        .onReceive(viewModel.event) { event in
            switch event {
            case .done:
                onBack()
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

private struct CoinChargeContentView: View {
    let uiState: CoinChargeUiState

    let onAction: (CoinChargeAction) -> Void

    let onBack: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            topBar
            content.padding(.top, 20)
        }
        .padding(.horizontal, 20)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(PassmateColors.surface.ignoresSafeArea())
    }

    private var topBar: some View {
        HStack(spacing: 12) {
            Button(action: onBack) {
                Text("←").font(.system(size: 22)).foregroundColor(PassmateColors.textPrimary)
            }
            Text("코인 충전")
                .font(.system(size: 20, weight: .bold))
                .kerning(-0.4)
                .foregroundColor(PassmateColors.textPrimary)
        }
        .padding(.top, 16)
    }

    @ViewBuilder
    private var content: some View {
        if uiState.isLoading {
            centerProgress
        } else if uiState.hasLoadError {
            retryState
        } else if uiState.isCompleted {
            completedBody
        } else {
            amountBody
        }
    }

    // MARK: - M-12-4 금액 선택

    private var amountBody: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                balanceCard
                sectionTitle("충전 금액").padding(.top, 24)
                amountGrid.padding(.top, 10)
                sectionTitle("결제 수단").padding(.top, 22)
                methodList.padding(.top, 10)
                Text("1 C = ₩1 · 포트원(PortOne) 안전 결제 · 충전 후 7일 내 미사용 시 환불 가능")
                    .font(.system(size: 12))
                    .kerning(-0.24)
                    .foregroundColor(PassmateColors.textTertiary)
                    .padding(.top, 12)
                if let errorMessage = uiState.errorMessage {
                    Text(errorMessage)
                        .font(.system(size: 13))
                        .foregroundColor(PassmateColors.wrongPinkText)
                        .padding(.top, 12)
                }
                chargeButton.padding(.top, 20)
            }
            .padding(.bottom, 24)
        }
    }

    private func sectionTitle(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 15, weight: .bold))
            .foregroundColor(PassmateColors.textPrimary)
    }

    private var balanceCard: some View {
        HStack(spacing: 10) {
            Text("C")
                .font(.system(size: 12, weight: .bold))
                .foregroundColor(PassmateColors.inkGreen)
                .frame(width: 22, height: 22)
                .overlay(Circle().stroke(PassmateColors.inkGreen, lineWidth: 1.5))
            Text("보유 코인")
                .font(.system(size: 15))
                .foregroundColor(PassmateColors.textSecondary)
            Spacer()
            Text("\(formatNumber(uiState.balance)) C")
                .font(.system(size: 22, weight: .bold))
                .kerning(-0.44)
                .foregroundColor(PassmateColors.primaryDeep)
        }
        .padding(.horizontal, 22)
        .padding(.vertical, 28)
        .frame(maxWidth: .infinity)
        .background(PassmateColors.backgroundMint)
        .cornerRadius(18)
    }

    // 시안 M-12-4의 2×2 그리드 — 프리셋 개수가 홀수여도 마지막 줄이 깨지지 않게 chunked로 채운다
    private var amountGrid: some View {
        VStack(spacing: 10) {
            ForEach(amountRows, id: \.first) { row in
                HStack(spacing: 10) {
                    ForEach(row, id: \.self) { amount in
                        amountCell(amount: amount)
                    }
                    if row.count == 1 {
                        Color.clear.frame(maxWidth: .infinity)
                    }
                }
            }
        }
    }

    private var amountRows: [[Int]] {
        stride(from: 0, to: uiState.presets.count, by: 2).map { start in
            Array(uiState.presets[start..<min(start + 2, uiState.presets.count)])
        }
    }

    private func amountCell(amount: Int) -> some View {
        let isSelected = amount == uiState.selectedAmount

        return Button {
            onAction(.selectAmount(amount: amount))
        } label: {
            Text("₩\(formatNumber(amount))")
                .font(.system(size: 15, weight: isSelected ? .bold : .medium))
                .foregroundColor(isSelected ? PassmateColors.primaryDeep : PassmateColors.textPrimary)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 18)
                .overlay(
                    RoundedRectangle(cornerRadius: 14).stroke(
                        isSelected ? PassmateColors.primary : PassmateColors.border,
                        lineWidth: isSelected ? 1.5 : 1
                    )
                )
        }
    }

    private var methodList: some View {
        VStack(spacing: 10) {
            ForEach(paymentMethods, id: \.self) { method in
                methodRow(method: method)
            }
        }
    }

    private var paymentMethods: [PaymentMethod] {
        [.kakaoPay, .naverPay, .tossPay, .card, .transfer]
    }

    private func methodRow(method: PaymentMethod) -> some View {
        let isSelected = method == uiState.selectedMethod

        return Button {
            onAction(.selectMethod(method: method))
        } label: {
            HStack(spacing: 12) {
                radioMark(isSelected: isSelected)
                Text(method.label)
                    .font(.system(size: 15, weight: isSelected ? .bold : .regular))
                    .foregroundColor(isSelected ? PassmateColors.textPrimary : PassmateColors.textSecondary)
                Spacer()
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 16)
            .overlay(
                RoundedRectangle(cornerRadius: 14).stroke(
                    isSelected ? PassmateColors.primary : PassmateColors.border,
                    lineWidth: isSelected ? 1.5 : 1
                )
            )
        }
    }

    private func radioMark(isSelected: Bool) -> some View {
        ZStack {
            Circle().stroke(isSelected ? PassmateColors.primary : PassmateColors.border, lineWidth: 1.5)
            if isSelected {
                Circle().fill(PassmateColors.primary).frame(width: 10, height: 10)
            }
        }
        .frame(width: 20, height: 20)
    }

    private var chargeButton: some View {
        Button {
            onAction(.clickCharge)
        } label: {
            Group {
                if uiState.isProcessing {
                    ProgressView().tint(PassmateColors.surface)
                } else {
                    Text("₩\(formatNumber(uiState.selectedAmount)) 충전하기")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(PassmateColors.surface)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 18)
            .background(uiState.isProcessing ? PassmateColors.border : PassmateColors.primary)
            .cornerRadius(16)
        }
        .disabled(uiState.isProcessing)
    }

    // MARK: - M-12-6 충전 완료

    private var completedBody: some View {
        VStack(spacing: 0) {
            Spacer()
            Text("✓")
                .font(.system(size: 34, weight: .bold))
                .foregroundColor(PassmateColors.surface)
                .frame(width: 72, height: 72)
                .background(Circle().fill(PassmateColors.primary))
            Text("\(formatNumber(uiState.chargedAmount)) C 충전 완료")
                .font(.system(size: 22, weight: .bold))
                .kerning(-0.44)
                .foregroundColor(PassmateColors.textPrimary)
                .padding(.top, 20)
            Text("보유 코인 \(formatNumber(uiState.balance)) C · \(uiState.selectedMethod.label) ₩\(formatNumber(uiState.chargedAmount))")
                .font(.system(size: 14))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.textSecondary)
                .padding(.top, 10)
            Text("결제 내역은 마이 › 코인 · 결제에서 볼 수 있어요")
                .font(.system(size: 13))
                .kerning(-0.26)
                .foregroundColor(PassmateColors.textTertiary)
                .padding(.top, 8)
            Button {
                onAction(.clickConfirmDone)
            } label: {
                Text("확인")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(PassmateColors.surface)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 18)
                    .background(PassmateColors.primary)
                    .cornerRadius(16)
            }
            .padding(.top, 28)
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - 공통 상태

    private var centerProgress: some View {
        VStack {
            Spacer()
            ProgressView().tint(PassmateColors.primary)
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }

    private var retryState: some View {
        VStack(spacing: 12) {
            Spacer()
            Text("보유 코인을 불러오지 못했어요")
                .font(.system(size: 14))
                .foregroundColor(PassmateColors.textSecondary)
            Button {
                onAction(.retry)
            } label: {
                Text("다시 시도")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(PassmateColors.primaryDeep)
                    .padding(.horizontal, 18)
                    .padding(.vertical, 10)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(PassmateColors.border, lineWidth: 1))
            }
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }

    private func formatNumber(_ value: Int) -> String {
        let digits = Array(String(value))
        var result = ""

        for (index, digit) in digits.enumerated() {
            if index > 0 && (digits.count - index) % 3 == 0 {
                result.append(",")
            }
            result.append(digit)
        }
        return result
    }
}

// MARK: - 프리뷰 (Figma 시안 비교용, Koin 미초기화 상태에서도 안전한 콘텐츠 뷰 기반)

#Preview("M-12-4 금액 선택") {
    CoinChargeContentView(
        uiState: CoinChargeUiState(
            isLoading: false,
            balance: 1200,
            presets: [5_000, 10_000, 30_000, 50_000],
            selectedAmount: 10_000,
            selectedMethod: .kakaoPay
        ),
        onAction: { _ in },
        onBack: {}
    )
}

#Preview("M-12-6 충전 완료") {
    CoinChargeContentView(
        uiState: CoinChargeUiState(
            isLoading: false,
            balance: 11_200,
            selectedMethod: .kakaoPay,
            isCompleted: true,
            chargedAmount: 10_000
        ),
        onAction: { _ in },
        onBack: {}
    )
}
