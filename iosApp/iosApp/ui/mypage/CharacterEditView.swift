import SwiftUI
import Shared

// Figma "UI 디자인 v6" M-12-7(450:5938) 미러 — 내 캐릭터 변경. 4열 x 3행 그리드 + 선택 라벨 + 저장.
// 시안이 전체 페이지라 라우트 push로 띄운다 (규칙 §2-1 — 상세는 모달이 아니라 push)
struct CharacterEditView: View {
    @StateObject private var viewModel = CharacterEditViewModel(
        getMyProfileUseCase: KoinHelper.shared.getMyProfileUseCase(),
        updateMyProfileUseCase: KoinHelper.shared.updateMyProfileUseCase()
    )

    var onBack: () -> Void = {}

    @State private var noticeMessage: String?

    var body: some View {
        CharacterEditContentView(
            uiState: viewModel.uiState,
            onAction: { viewModel.action($0) },
            onBack: onBack
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

private struct CharacterEditContentView: View {
    let uiState: CharacterEditUiState

    let onAction: (CharacterEditAction) -> Void

    let onBack: () -> Void

    private let columns = 4

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            topBar
            Text("대기실 · 결과 화면에서 닉네임과 함께 보여요")
                .font(.system(size: 14))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.textSecondary)
            if uiState.isLoading {
                loadingBox
            } else if uiState.hasLoadError {
                retryBox
            } else {
                avatarGrid
                Text("선택: \(StudentAvatars.nameOf(uiState.avatarId ?? StudentAvatars.defaultId))")
                    .font(.system(size: 14, weight: .bold))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.primary)
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
            Text("내 캐릭터")
                .font(.system(size: 20, weight: .bold))
                .kerning(-0.4)
                .foregroundColor(PassmateColors.textPrimary)
            Spacer()
        }
        .padding(.top, 16)
    }

    private var avatarGrid: some View {
        VStack(spacing: 12) {
            ForEach(0..<(StudentAvatars.count / columns), id: \.self) { row in
                HStack(spacing: 12) {
                    ForEach(0..<columns, id: \.self) { col in
                        let avatarId = row * columns + col + 1
                        let isSelected = avatarId == uiState.avatarId

                        Button {
                            onAction(.selectAvatar(avatarId: avatarId))
                        } label: {
                            StudentAvatarView(avatarId: avatarId)
                                .frame(width: 52, height: 52)
                                .frame(maxWidth: .infinity)
                                .frame(height: 84)
                                .background(isSelected ? PassmateColors.backgroundMint : PassmateColors.surface)
                                .cornerRadius(16)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 16)
                                        .stroke(
                                            isSelected ? PassmateColors.primary : PassmateColors.border,
                                            lineWidth: isSelected ? 2 : 1
                                        )
                                )
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
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
            Text("캐릭터를 불러오지 못했어요")
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

struct CharacterEditView_Previews: PreviewProvider {
    static var previews: some View {
        CharacterEditContentView(
            uiState: CharacterEditUiState(avatarId: 1, isLoading: false),
            onAction: { _ in },
            onBack: {}
        )
    }
}
