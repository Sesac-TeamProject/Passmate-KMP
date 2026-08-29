import SwiftUI
import Shared

// Figma "UI 디자인 v6" M-12-3(437:5534) 미러 — 정산 계좌 등록/변경: 은행·계좌번호·예금주.
// 시트 표시 여부는 호스팅 화면(EarningsView)이 소유한다 (규칙 §11-1)
struct SettlementAccountSheetView: View {
    var onSaved: () -> Void = {}

    var onNotice: (String) -> Void = { _ in }

    var onClose: () -> Void = {}

    @StateObject private var viewModel = SettlementAccountViewModel(
        getSettlementAccountUseCase: KoinHelper.shared.getSettlementAccountUseCase(),
        saveSettlementAccountUseCase: KoinHelper.shared.saveSettlementAccountUseCase()
    )

    var body: some View {
        SettlementAccountContentView(
            uiState: viewModel.uiState,
            onAction: { viewModel.action($0) },
            onClose: onClose
        )
        .onAppear {
            viewModel.action(.enter)
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .saved:
                onSaved()
            case let .showNotice(message):
                onNotice(message)
            }
        }
    }
}

private struct SettlementAccountContentView: View {
    let uiState: SettlementAccountUiState

    let onAction: (SettlementAccountAction) -> Void

    let onClose: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                HStack {
                    Text("정산 계좌")
                        .font(.system(size: 20, weight: .bold))
                        .kerning(-0.4)
                        .foregroundColor(PassmateColors.textPrimary)
                    Spacer()
                    Button(action: onClose) {
                        Text("✕")
                            .font(.system(size: 18))
                            .foregroundColor(PassmateColors.textSecondary)
                    }
                }
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
