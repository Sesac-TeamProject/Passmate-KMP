import SwiftUI
import Shared

// 설정 — 마이 탭 우상단 "설정"에서 push. 회원 탈퇴(M-12-12, 확인 알림)만 둔다
struct SettingsView: View {
    var onRequireSignIn: () -> Void = {}

    var onAccountDeleted: () -> Void = {}

    var onBack: () -> Void = {}

    @StateObject private var viewModel = SettingsViewModel(
        deleteAccountUseCase: KoinHelper.shared.deleteAccountUseCase(),
        isSignedInUseCase: KoinHelper.shared.isSignedInUseCase()
    )

    @State private var showDeleteConfirm = false

    @State private var noticeMessage: String?

    var body: some View {
        SettingsContentView(
            uiState: viewModel.uiState,
            onClickBack: onBack,
            onClickDelete: { showDeleteConfirm = true }
        )
        .onAppear {
            viewModel.action(.enter)
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .requireSignIn:
                onRequireSignIn()
            case .accountDeleted:
                onAccountDeleted()
            case let .showNotice(message):
                noticeMessage = message
            }
        }
        .alert("회원 탈퇴", isPresented: $showDeleteConfirm) {
            Button("탈퇴", role: .destructive) {
                viewModel.action(.confirmDeleteAccount)
            }
            Button("취소", role: .cancel) {}
        } message: {
            Text("탈퇴하면 참여·개설 기록과 보유 코인이 모두 삭제되고 되돌릴 수 없어요. 정산 대기 금액이나 진행 중인 방이 있으면 탈퇴할 수 없어요.")
        }
        .overlay(alignment: .bottom) {
            if let noticeMessage {
                SettingsNoticeToast(message: noticeMessage)
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                            self.noticeMessage = nil
                        }
                    }
            }
        }
    }
}

private struct SettingsNoticeToast: View {
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

private struct SettingsContentView: View {
    let uiState: SettingsUiState

    let onClickBack: () -> Void

    let onClickDelete: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text("설정")
                    .font(.system(size: 24, weight: .bold))
                    .kerning(-0.48)
                    .foregroundColor(PassmateColors.textPrimary)
                Spacer()
                Button(action: onClickBack) {
                    Text("닫기")
                        .font(.system(size: 14, weight: .medium))
                        .kerning(-0.28)
                        .foregroundColor(PassmateColors.textSecondary)
                }
            }
            Button(action: onClickDelete) {
                HStack {
                    Text("회원 탈퇴")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(PassmateColors.weakTopicText)
                    Spacer()
                    Text("›").font(.system(size: 18)).foregroundColor(PassmateColors.textTertiary)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 16)
                .background(PassmateColors.surface)
                .overlay(RoundedRectangle(cornerRadius: 16).stroke(PassmateColors.border, lineWidth: 1))
            }
            if uiState.isProcessing {
                HStack {
                    Spacer()
                    ProgressView().tint(PassmateColors.primary)
                    Spacer()
                }
            }
            Spacer()
        }
        .padding(.horizontal, 20)
        .padding(.top, 32)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(PassmateColors.surface.ignoresSafeArea())
    }
}

// MARK: - 프리뷰 (Figma 시안 비교용)
#Preview("설정") {
    SettingsContentView(uiState: SettingsUiState(), onClickBack: {}, onClickDelete: {})
}
