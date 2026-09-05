import SwiftUI
import Shared

// Figma "UI 디자인 v6" M-12-3(437:5534) 미러 — 정산 계좌 등록/변경: 은행·계좌번호·예금주.
// 시안이 전체 페이지라 라우트 push로 띄운다 (규칙 §2-1 — 상세는 모달이 아니라 push)
struct SettlementAccountView: View {
    var onBack: () -> Void = {}

    @State private var noticeMessage: String?

    @StateObject private var viewModel = SettlementAccountViewModel(
        getSettlementAccountUseCase: KoinHelper.shared.getSettlementAccountUseCase(),
        saveSettlementAccountUseCase: KoinHelper.shared.saveSettlementAccountUseCase()
    )

    var body: some View {
        SettlementAccountContentView(
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

private struct SettlementAccountContentView: View {
    let uiState: SettlementAccountUiState

    let onAction: (SettlementAccountAction) -> Void

    let onBack: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                HStack(spacing: 12) {
                    PassmateBackButton(onClick: onBack)
                    Text("정산 계좌 등록")
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
                    .frame(height: 180)
                } else {
                    accountField(
                        label: "은행",
                        placeholder: "예: 신한은행",
                        value: uiState.bankName,
                        onChange: { onAction(.changeBankName(text: $0)) }
                    )
                    accountField(
                        label: "계좌번호",
                        placeholder: "숫자만 입력",
                        value: uiState.accountNumber,
                        keyboardType: .numberPad,
                        onChange: { onAction(.changeAccountNumber(text: $0)) }
                    )
                    accountField(
                        label: "예금주",
                        placeholder: "예금주명",
                        value: uiState.holderName,
                        onChange: { onAction(.changeHolderName(text: $0)) }
                    )
                    Text("매월 5일 지급 · 사업소득 3.3% 원천징수(확정 전)")
                        .font(.system(size: 12))
                        .kerning(-0.24)
                        .foregroundColor(PassmateColors.textTertiary)
                    saveButton
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 24)
            .padding(.bottom, 28)
        }
        .background(PassmateColors.surface.ignoresSafeArea())
    }

    private func accountField(
        label: String,
        placeholder: String,
        value: String,
        keyboardType: UIKeyboardType = .default,
        onChange: @escaping (String) -> Void
    ) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(.system(size: 13, weight: .medium))
                .kerning(-0.26)
                .foregroundColor(PassmateColors.textSecondary)
            TextField(placeholder, text: Binding(
                get: { value },
                set: { onChange($0) }
            ))
            .font(.system(size: 14))
            .keyboardType(keyboardType)
            .padding(16)
            .background(PassmateColors.fieldGray)
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
