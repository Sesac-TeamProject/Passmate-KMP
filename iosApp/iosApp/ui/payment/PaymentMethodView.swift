import SwiftUI
import Shared

// Figma "UI 디자인 v6" M-12-8 미러 — 결제 수단 관리: 기본 수단 5종 선택(카드 정보는 포트원 처리).
// 시안이 전체 페이지라 라우트 push로 띄운다 (규칙 §2-1 — 상세는 모달이 아니라 push)
struct PaymentMethodView: View {
    var onBack: () -> Void = {}

    @State private var noticeMessage: String?

    @StateObject private var viewModel = PaymentMethodViewModel(
        getMyCoinsUseCase: KoinHelper.shared.getMyCoinsUseCase(),
        setPaymentMethodUseCase: KoinHelper.shared.setPaymentMethodUseCase()
    )

    var body: some View {
        PaymentMethodContentView(
            uiState: viewModel.uiState,
            onAction: { viewModel.action($0) },
            onBack: onBack
        )
        .onAppear {
            viewModel.action(.enter)
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .saved:
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

private struct PaymentMethodContentView: View {
    let uiState: PaymentMethodUiState

    let onAction: (PaymentMethodAction) -> Void

    let onBack: () -> Void

    private let methods: [PaymentMethod] = [.kakaoPay, .naverPay, .tossPay, .card, .transfer]

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                PassmateBackButton(onClick: onBack)
                Text("결제 수단 관리")
                    .font(.system(size: 20, weight: .bold))
                    .kerning(-0.4)
                    .foregroundColor(PassmateColors.textPrimary)
                Spacer()
            }
            .padding(.top, 16)
            if uiState.isLoading {
                HStack {
                    Spacer()
                    ProgressView().tint(PassmateColors.primary)
                    Spacer()
                }
                .frame(height: 160)
            } else {
                Text("코인 충전 시 기본으로 선택될 수단이에요 · 카드 정보는 저장되지 않아요")
                    .font(.system(size: 12))
                    .kerning(-0.24)
                    .foregroundColor(PassmateColors.textTertiary)
                ForEach(methods, id: \.self) { method in
                    methodRow(method)
                }
                saveButton
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, 24)
        .padding(.bottom, 28)
        .background(PassmateColors.surface.ignoresSafeArea())
    }

    private func methodRow(_ method: PaymentMethod) -> some View {
        let isSelected = method == uiState.selected

        return Button(action: { onAction(.select(method: method)) }) {
            HStack(spacing: 10) {
                Circle()
                    .strokeBorder(
                        isSelected ? PassmateColors.primary : PassmateColors.border,
                        lineWidth: isSelected ? 5 : 1
                    )
                    .frame(width: 18, height: 18)
                Text(method.label)
                    .font(.system(size: 15, weight: .medium))
                    .kerning(-0.3)
                    .foregroundColor(PassmateColors.textPrimary)
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(isSelected ? PassmateColors.backgroundMint : PassmateColors.surface)
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .stroke(isSelected ? PassmateColors.primary : PassmateColors.border, lineWidth: 1)
            )
            .cornerRadius(14)
        }
    }

    private var saveButton: some View {
        Button {
            onAction(.submit)
        } label: {
            Group {
                if uiState.isSubmitting {
                    ProgressView().tint(PassmateColors.surface)
                } else {
                    Text("저장")
                        .font(.system(size: 15, weight: .bold))
                        .kerning(-0.3)
                        .foregroundColor(PassmateColors.surface)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(uiState.canSubmit ? PassmateColors.primary : PassmateColors.border)
            .cornerRadius(16)
        }
        .disabled(!uiState.canSubmit)
    }
}
