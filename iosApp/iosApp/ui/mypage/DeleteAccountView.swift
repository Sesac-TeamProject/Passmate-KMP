import SwiftUI
import Shared

// 회원 탈퇴 (M-12-12) — Compose DeleteAccountScreen.kt 미러.
// 설정에서 push로 진입한다 (이전에는 설정 안의 확인 알림이었다)
struct DeleteAccountView: View {
    @StateObject private var viewModel = DeleteAccountViewModel(
        getMyCoinsUseCase: KoinHelper.shared.getMyCoinsUseCase(),
        deleteAccountUseCase: KoinHelper.shared.deleteAccountUseCase()
    )

    var onAccountDeleted: () -> Void = {}

    var onBack: () -> Void = {}

    @State private var noticeMessage: String?

    var body: some View {
        DeleteAccountContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.action,
            onBack: onBack
        )
        .onAppear { viewModel.action(.enter) }
        .onReceive(viewModel.event) { event in
            switch event {
            case .deleted:
                onAccountDeleted()
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

private struct DeleteAccountContentView: View {
    let uiState: DeleteAccountUiState

    let onAction: (DeleteAccountAction) -> Void

    let onBack: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            topBar
            noticeCard.padding(.top, 20)
            Text("정산 예정 금액이 있으면 지급이 끝난 뒤 탈퇴할 수 있어요.")
                .font(.system(size: 12))
                .kerning(-0.24)
                .foregroundColor(PassmateColors.textTertiary)
                .padding(.top, 14)
            confirmRow.padding(.top, 16)
            Spacer()
            deleteButton.padding(.bottom, 24)
        }
        .padding(.horizontal, 20)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(PassmateColors.surface.ignoresSafeArea())
    }

    private var topBar: some View {
        HStack(spacing: 12) {
            PassmateBackButton(onClick: onBack)
            Text("회원 탈퇴")
                .font(.system(size: 20, weight: .bold))
                .kerning(-0.4)
                .foregroundColor(PassmateColors.textPrimary)
        }
        .padding(.top, 16)
    }

    private var noticeCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("탈퇴하면 아래 내용이 모두 삭제돼요")
                .font(.system(size: 15, weight: .bold))
                .kerning(-0.3)
                .foregroundColor(PassmateColors.textPrimary)
            bulletLine("참여 기록 · 뱃지 · 명성 등급")
            bulletLine("보유 코인 \(formatNumber(uiState.coins)) C (환불되지 않아요)")
            bulletLine("내가 만든 방 · 문제 세트")
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(PassmateColors.fieldGray)
        .cornerRadius(16)
    }

    private func bulletLine(_ text: String) -> some View {
        HStack(spacing: 10) {
            Circle().fill(PassmateColors.textTertiary).frame(width: 4, height: 4)
            Text(text)
                .font(.system(size: 14))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.textSecondary)
        }
    }

    private var confirmRow: some View {
        Button {
            onAction(.toggleConfirm)
        } label: {
            HStack(spacing: 10) {
                checkMark
                Text("위 내용을 확인했어요")
                    .font(.system(size: 15))
                    .kerning(-0.3)
                    .foregroundColor(PassmateColors.textPrimary)
                Spacer()
            }
            .padding(.vertical, 4)
        }
    }

    private var checkMark: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 7)
                .fill(uiState.isConfirmed ? PassmateColors.primary : PassmateColors.surface)
            if uiState.isConfirmed {
                Text("✓").font(.system(size: 14, weight: .bold)).foregroundColor(PassmateColors.surface)
            } else {
                RoundedRectangle(cornerRadius: 7).stroke(PassmateColors.border, lineWidth: 1.5)
            }
        }
        .frame(width: 24, height: 24)
    }

    // 파괴적 동작이라 시안이 primary 대신 검정 계열을 쓴다 — 토큰 중 textPrimary가 그 톤이다 (규칙 §11-2)
    private var deleteButton: some View {
        Button {
            onAction(.clickDelete)
        } label: {
            Group {
                if uiState.isProcessing {
                    ProgressView().tint(PassmateColors.surface)
                } else {
                    Text("탈퇴하기")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(PassmateColors.surface)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 18)
            .background(uiState.canDelete ? PassmateColors.textPrimary : PassmateColors.border)
            .cornerRadius(16)
        }
        .disabled(!uiState.canDelete)
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

// MARK: - 프리뷰 (Figma 시안 비교용)

#Preview("M-12-12 회원 탈퇴 (체크 완료)") {
    DeleteAccountContentView(
        uiState: DeleteAccountUiState(isLoading: false, coins: 1200, isConfirmed: true),
        onAction: { _ in },
        onBack: {}
    )
}

#Preview("M-12-12 회원 탈퇴 (미체크)") {
    DeleteAccountContentView(
        uiState: DeleteAccountUiState(isLoading: false, coins: 1200, isConfirmed: false),
        onAction: { _ in },
        onBack: {}
    )
}
