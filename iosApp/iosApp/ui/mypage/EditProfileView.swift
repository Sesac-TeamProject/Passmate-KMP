import SwiftUI
import Shared

// Figma "UI 디자인 v6" M-12-1(437:5424) 미러 — 계정 정보 변경: 캐릭터 요약 + 닉네임 + 이메일(읽기 전용).
// 캐릭터 자체는 M-12-7(CharacterEditView)에서 바꾼다. 시안이 전체 페이지라 라우트 push다 (규칙 §2-1)
struct EditProfileView: View {
    @StateObject private var viewModel = EditProfileViewModel(
        getMyProfileUseCase: KoinHelper.shared.getMyProfileUseCase(),
        updateMyProfileUseCase: KoinHelper.shared.updateMyProfileUseCase()
    )

    var onBack: () -> Void = {}

    var onOpenCharacterEdit: () -> Void = {}

    @State private var noticeMessage: String?

    var body: some View {
        EditProfileContentView(
            uiState: viewModel.uiState,
            onAction: { viewModel.action($0) },
            onBack: onBack,
            onOpenCharacterEdit: onOpenCharacterEdit
        )
        .onAppear { viewModel.action(.enter) }
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

private struct EditProfileContentView: View {
    let uiState: EditProfileUiState

    let onAction: (EditProfileAction) -> Void

    let onBack: () -> Void

    let onOpenCharacterEdit: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            topBar
            if uiState.isLoading {
                loadingBox
            } else if uiState.hasLoadError {
                retryBox
            } else {
                profileCard
                saveButton
            }
            Spacer()
        }
        .padding(.horizontal, 20)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(PassmateColors.surface.ignoresSafeArea())
    }

    private var topBar: some View {
        HStack(spacing: 12) {
            PassmateBackButton(onClick: onBack)
            Text("계정 정보 변경")
                .font(.system(size: 20, weight: .bold))
                .kerning(-0.4)
                .foregroundColor(PassmateColors.textPrimary)
            Spacer()
        }
        .padding(.top, 16)
    }

    private var profileCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 14) {
                StudentAvatarView(avatarId: uiState.avatarId ?? StudentAvatars.defaultId)
                    .frame(width: 56, height: 56)
                VStack(alignment: .leading, spacing: 4) {
                    Text("프로필 캐릭터")
                        .font(.system(size: 15, weight: .bold))
                        .kerning(-0.3)
                        .foregroundColor(PassmateColors.textPrimary)
                    Button("캐릭터 바꾸기 →") { onOpenCharacterEdit() }
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(PassmateColors.primary)
                }
                Spacer()
            }
            fieldLabel("닉네임")
            TextField("닉네임 (최대 12자)", text: Binding(
                get: { uiState.nickname },
                set: { onAction(.changeNickname(text: $0)) }
            ))
            .font(.system(size: 14))
            .foregroundColor(PassmateColors.textPrimary)
            .padding(.horizontal, 16)
            .frame(height: 52)
            .background(PassmateColors.fieldGray)
            .cornerRadius(14)
            fieldLabel("이메일")
            Text(uiState.email ?? "-")
                .font(.system(size: 14))
                .foregroundColor(PassmateColors.textTertiary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 16)
                .frame(height: 52)
                .background(PassmateColors.fieldGray)
                .cornerRadius(14)
            Text("이메일은 로그인 ID라 바꿀 수 없어요. 닉네임은 방 안에서 학생 · 선생님에게 보여요")
                .font(.system(size: 12))
                .kerning(-0.24)
                .foregroundColor(PassmateColors.textTertiary)
        }
        .padding(18)
        .overlay(
            RoundedRectangle(cornerRadius: 18)
                .stroke(PassmateColors.border, lineWidth: 1)
        )
    }

    private func fieldLabel(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 13, weight: .medium))
            .kerning(-0.26)
            .foregroundColor(PassmateColors.textSecondary)
    }

    private var loadingBox: some View {
        HStack {
            Spacer()
            ProgressView().tint(PassmateColors.primary)
            Spacer()
        }
        .frame(height: 220)
    }

    private var retryBox: some View {
        VStack(spacing: 8) {
            Text("계정 정보를 불러오지 못했어요")
                .font(.system(size: 14))
                .foregroundColor(PassmateColors.textSecondary)
            Button("다시 시도") { onAction(.retry) }
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(PassmateColors.primary)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 220)
    }

    private var saveButton: some View {
        Button {
            onAction(.submit)
        } label: {
            ZStack {
                if uiState.isSubmitting {
                    ProgressView().tint(PassmateColors.surface)
                } else {
                    Text("저장하기")
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
        .buttonStyle(.plain)
        .disabled(!uiState.canSubmit)
    }
}

struct EditProfileView_Previews: PreviewProvider {
    static var previews: some View {
        EditProfileContentView(
            uiState: EditProfileUiState(
                nickname: "한결",
                email: "hangyeol@example.com",
                avatarId: 1,
                isLoading: false
            ),
            onAction: { _ in },
            onBack: {},
            onOpenCharacterEdit: {}
        )
    }
}
