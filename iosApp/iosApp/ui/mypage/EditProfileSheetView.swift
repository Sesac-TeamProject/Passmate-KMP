import SwiftUI
import Shared

// Figma "UI 디자인 v6" M-12-1+M-12-7 미러 — 계정 정보(닉네임)·내 캐릭터 변경 통합 시트.
// 시트 표시 여부는 호스팅 화면(SettingsView)이 소유한다 (규칙 §11-1)
struct EditProfileSheetView: View {
    let initialNickname: String

    let initialAvatarId: Int?

    var onSaved: () -> Void = {}

    var onNotice: (String) -> Void = { _ in }

    var onClose: () -> Void = {}

    @StateObject private var viewModel = EditProfileViewModel(
        updateMyProfileUseCase: KoinHelper.shared.updateMyProfileUseCase()
    )

    var body: some View {
        EditProfileContentView(
            uiState: viewModel.uiState,
            onAction: { viewModel.action($0) },
            onClose: onClose
        )
        .onAppear {
            viewModel.action(.enter(nickname: initialNickname, avatarId: initialAvatarId))
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

private struct EditProfileContentView: View {
    let uiState: EditProfileUiState

    let onAction: (EditProfileAction) -> Void

    let onClose: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                HStack {
                    Text("계정 정보")
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
                Text("닉네임")
                    .font(.system(size: 13, weight: .medium))
                    .kerning(-0.26)
                    .foregroundColor(PassmateColors.textSecondary)
                TextField("닉네임 (최대 12자)", text: Binding(
                    get: { uiState.nickname },
                    set: { onAction(.changeNickname(text: $0)) }
                ))
                .font(.system(size: 14))
                .padding(16)
                .background(PassmateColors.fieldGray)
                .cornerRadius(14)
                Text("내 캐릭터 — 대기실·결과 화면에 표시돼요")
                    .font(.system(size: 13, weight: .medium))
                    .kerning(-0.26)
                    .foregroundColor(PassmateColors.textSecondary)
                avatarGrid
                saveButton
            }
            .padding(.horizontal, 20)
            .padding(.top, 24)
            .padding(.bottom, 28)
        }
        .background(PassmateColors.surface.ignoresSafeArea())
    }

    private var avatarGrid: some View {
        VStack(spacing: 10) {
            ForEach(0..<2, id: \.self) { row in
                HStack(spacing: 10) {
                    ForEach(1...6, id: \.self) { col in
                        let avatarId = row * 6 + col
                        let isSelected = avatarId == uiState.avatarId

                        Button(action: { onAction(.selectAvatar(avatarId: avatarId)) }) {
                            StudentAvatarView(avatarId: avatarId)
                                .frame(width: 40, height: 40)
                                .padding(4)
                                .overlay(
                                    Circle().stroke(
                                        isSelected ? PassmateColors.primary : PassmateColors.border,
                                        lineWidth: isSelected ? 2 : 1
                                    )
                                )
                        }
                        .frame(maxWidth: .infinity)
                    }
                }
            }
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
