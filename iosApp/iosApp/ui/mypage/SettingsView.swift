import SwiftUI
import Shared

// 설정 — 마이 탭 우상단 "설정"에서 push. 회원 탈퇴는 전용 화면(M-12-12)으로 push한다
struct SettingsView: View {
    var onRequireSignIn: () -> Void = {}

    var onOpenDeleteAccount: () -> Void = {}

    var onBack: () -> Void = {}

    @StateObject private var viewModel = SettingsViewModel(
        isSignedInUseCase: KoinHelper.shared.isSignedInUseCase()
    )

    var body: some View {
        SettingsContentView(
            onClickBack: onBack,
            onClickDelete: onOpenDeleteAccount
        )
        .onAppear {
            viewModel.action(.enter)
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .requireSignIn:
                onRequireSignIn()
            }
        }
    }
}

private struct SettingsContentView: View {
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
    SettingsContentView(onClickBack: {}, onClickDelete: {})
}
