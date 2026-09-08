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
            VStack(alignment: .leading, spacing: 16) {
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
                    formCard
                    registerButton
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 24)
            .padding(.bottom, 28)
        }
        .background(PassmateColors.surface.ignoresSafeArea())
    }

    // 시안 card — 필드 3개 + 안내문을 테두리 카드(r16, pad 16, gap 16)로 감싼다
    private var formCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            bankSelectField
            accountField(
                label: "계좌번호",
                placeholder: uiState.maskedAccountNumber.isEmpty ? "숫자만 입력" : uiState.maskedAccountNumber,
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
            // 지급 주기는 시안끼리 어긋난다(M-12-3 "매주 월요일" vs M-T4 "매월 5일") — 앱은 매월 5일로 통일
            Text("매월 5일 지급 · 사업소득 3.3% 원천징수(확정 전)")
                .font(.system(size: 12))
                .kerning(-0.24)
                .foregroundColor(PassmateColors.textSecondary)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(PassmateColors.surface)
        .cornerRadius(16)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(PassmateColors.border, lineWidth: 1))
    }

    // 시안 field/은행 select — 은행 목록(Bank)에서 고른다. 자유 입력이면 백엔드 필수값 bankCode를 못 채운다
    private var bankSelectField: some View {
        VStack(alignment: .leading, spacing: 8) {
            fieldLabel("은행")
            Menu {
                ForEach(Bank.companion.all, id: \.code) { bank in
                    Button(bank.displayName) {
                        onAction(.selectBank(bank: bank))
                    }
                }
            } label: {
                HStack(spacing: 0) {
                    Text(uiState.bankName.isEmpty ? "은행 선택" : uiState.bankName)
                        .font(.system(size: 14))
                        .kerning(-0.28)
                        .foregroundColor(uiState.bankName.isEmpty ? PassmateColors.textTertiary : PassmateColors.textPrimary)
                    Spacer()
                    PassmateIconView(icon: .chevronDown, tint: PassmateColors.textPrimary, size: 24)
                }
                .padding(.horizontal, 16)
                .frame(height: 48)
                .frame(maxWidth: .infinity)
                .background(PassmateColors.fieldGray)
                .cornerRadius(12)
            }
        }
    }

    // 시안 field input — 48 높이 · fieldGray · r12
    private func accountField(
        label: String,
        placeholder: String,
        value: String,
        keyboardType: UIKeyboardType = .default,
        onChange: @escaping (String) -> Void
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            fieldLabel(label)
            TextField(placeholder, text: Binding(
                get: { value },
                set: { onChange($0) }
            ))
            .font(.system(size: 14))
            .keyboardType(keyboardType)
            .padding(.horizontal, 16)
            .frame(height: 48)
            .background(PassmateColors.fieldGray)
            .cornerRadius(12)
        }
    }

    private func fieldLabel(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 14, weight: .medium))
            .kerning(-0.28)
            .foregroundColor(PassmateColors.textPrimary)
    }

    // 시안 button/등록하기 — 48 높이 · r12 · 14 Medium. 등록/변경 모두 같은 문구를 쓴다
    private var registerButton: some View {
        Button {
            onAction(.submit)
        } label: {
            Group {
                if uiState.isSubmitting {
                    ProgressView().tint(PassmateColors.surface)
                } else {
                    Text("등록하기")
                        .font(.system(size: 14, weight: .medium))
                        .kerning(-0.28)
                        .foregroundColor(PassmateColors.surface)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 48)
            .background(uiState.canSubmit ? PassmateColors.primary : PassmateColors.border)
            .cornerRadius(12)
        }
        .disabled(!uiState.canSubmit)
    }
}

#Preview("M-12-3 정산 계좌 등록") {
    SettlementAccountContentView(
        uiState: SettlementAccountUiState(
            isLoading: false,
            bankCode: "004",
            bankName: "국민은행",
            accountNumber: "123456-01-234567",
            holderName: "이한결"
        ),
        onAction: { _ in },
        onBack: {}
    )
}
