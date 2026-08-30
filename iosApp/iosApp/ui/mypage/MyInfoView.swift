import SwiftUI
import Shared

// 시트 3종 중 무엇이 열려 있는지 — 표시 여부는 이 화면이 소유한다 (규칙 §11-1)
private enum MyInfoSheet: Identifiable {
    case editProfile(nickname: String, avatarId: Int?)
    case paymentMethod
    case notifications

    var id: String {
        switch self {
        case .editProfile: return "editProfile"
        case .paymentMethod: return "paymentMethod"
        case .notifications: return "notifications"
        }
    }
}

// Figma "UI 디자인 v6" M-12(349:9683) 미러 — 마이 탭 루트: 프로필·계정·코인·정산·알림·로그아웃
struct MyInfoView: View {
    var onRequireSignIn: () -> Void = {}

    var onOpenCoinHistory: () -> Void = {}

    var onSignedOut: () -> Void = {}

    var onBack: () -> Void = {}

    @StateObject private var viewModel = MyInfoViewModel(
        getMyProfileUseCase: KoinHelper.shared.getMyProfileUseCase(),
        signOutUseCase: KoinHelper.shared.signOutUseCase(),
        deleteAccountUseCase: KoinHelper.shared.deleteAccountUseCase(),
        isSignedInUseCase: KoinHelper.shared.isSignedInUseCase()
    )

    @State private var activeSheet: MyInfoSheet?

    @State private var showSignOutConfirm = false

    @State private var showDeleteConfirm = false

    @State private var noticeMessage: String?

    var body: some View {
        MyInfoContentView(
            uiState: viewModel.uiState,
            onAction: { viewModel.action($0) },
            onClickBack: onBack,
            onClickSignOut: { showSignOutConfirm = true },
            onClickDelete: { showDeleteConfirm = true }
        )
        .onAppear {
            viewModel.action(.enter)
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .requireSignIn:
                onRequireSignIn()
            case let .openEditProfile(nickname, avatarId):
                activeSheet = .editProfile(nickname: nickname, avatarId: avatarId)
            case .openPaymentMethod:
                activeSheet = .paymentMethod
            case .openNotifications:
                activeSheet = .notifications
            case .openCoinHistory:
                onOpenCoinHistory()
            case .signedOut, .accountDeleted:
                onSignedOut()
            case let .showNotice(message):
                noticeMessage = message
            }
        }
        .sheet(item: $activeSheet) { sheet in
            sheetContent(sheet)
                .presentationDetents([.medium, .large])
        }
        .alert("로그아웃", isPresented: $showSignOutConfirm) {
            Button("로그아웃", role: .destructive) {
                viewModel.action(.confirmSignOut)
            }
            Button("취소", role: .cancel) {}
        } message: {
            Text("로그아웃하면 게스트로 전환돼요. 기록은 계정에 안전하게 보관돼요.")
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
                MyInfoNoticeToast(message: noticeMessage)
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                            self.noticeMessage = nil
                        }
                    }
            }
        }
    }

    @ViewBuilder
    private func sheetContent(_ sheet: MyInfoSheet) -> some View {
        switch sheet {
        case let .editProfile(nickname, avatarId):
            EditProfileSheetView(
                initialNickname: nickname,
                initialAvatarId: avatarId,
                onSaved: {
                    activeSheet = nil
                    viewModel.action(.profileUpdated)
                },
                onNotice: { viewModel.action(.notice(message: $0)) },
                onClose: { activeSheet = nil }
            )
        case .paymentMethod:
            PaymentMethodSheetView(
                onSaved: {
                    activeSheet = nil
                    viewModel.action(.notice(message: "기본 결제 수단을 저장했어요"))
                },
                onNotice: { viewModel.action(.notice(message: $0)) },
                onClose: { activeSheet = nil }
            )
        case .notifications:
            NotificationSettingsSheetView(
                onNotice: { viewModel.action(.notice(message: $0)) },
                onClose: { activeSheet = nil }
            )
        }
    }
}

private struct MyInfoNoticeToast: View {
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

private struct MyInfoContentView: View {
    let uiState: MyInfoUiState

    let onAction: (MyInfoAction) -> Void

    let onClickBack: () -> Void

    let onClickSignOut: () -> Void

    let onClickDelete: () -> Void

    var body: some View {
        Group {
            if uiState.isLoading {
                ProgressView()
                    .tint(PassmateColors.primary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if uiState.loadFailed {
                errorView
            } else {
                loadedView
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(PassmateColors.surface.ignoresSafeArea())
    }

    private var errorView: some View {
        VStack(spacing: 12) {
            Text("내 정보를 불러오지 못했어요")
                .font(.system(size: 16, weight: .medium))
                .kerning(-0.32)
                .foregroundColor(PassmateColors.textPrimary)
            Button {
                onAction(.retry)
            } label: {
                Text("다시 시도")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.primaryDeep)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var loadedView: some View {
        ScrollView {
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
                if let profile = uiState.profile {
                    ProfileCardView(profile: profile)
                }
                settingRow(label: "계정 정보 · 내 캐릭터") { onAction(.clickEditProfile) }
                settingRow(label: "결제 수단 관리") { onAction(.clickPaymentMethod) }
                settingRow(label: "알림 설정") { onAction(.clickNotifications) }
                settingRow(label: "코인·결제 내역") { onAction(.clickCoinHistory) }
                settingRow(label: "로그아웃", labelColor: PassmateColors.textSecondary, action: onClickSignOut)
                settingRow(label: "회원 탈퇴", labelColor: PassmateColors.weakTopicText, action: onClickDelete)
                if uiState.isProcessing {
                    HStack {
                        Spacer()
                        ProgressView().tint(PassmateColors.primary)
                        Spacer()
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 32)
            .padding(.bottom, 24)
        }
    }

    private func settingRow(
        label: String,
        labelColor: Color = PassmateColors.textPrimary,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack {
                Text(label)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(labelColor)
                Spacer()
                Text("›").font(.system(size: 18)).foregroundColor(PassmateColors.textTertiary)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 16)
            .background(PassmateColors.surface)
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(PassmateColors.border, lineWidth: 1))
        }
    }
}

private struct ProfileCardView: View {
    let profile: UserProfile

    var body: some View {
        HStack(spacing: 14) {
            StudentAvatarView(avatarId: profile.avatarId.map { Int(truncating: $0) } ?? 0)
                .frame(width: 52, height: 52)
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 8) {
                    Text(profile.nickname)
                        .font(.system(size: 18, weight: .bold))
                        .kerning(-0.36)
                        .foregroundColor(PassmateColors.textPrimary)
                    if let level = localLevel {
                        ReputationBadgeView(level: level)
                    }
                }
                Text(subtitle)
                    .font(.system(size: 13))
                    .kerning(-0.26)
                    .foregroundColor(PassmateColors.textSecondary)
                if let coins = profile.coins?.int64Value {
                    Text("보유 코인 \(formatCoins(coins)) C")
                        .font(.system(size: 13, weight: .medium))
                        .kerning(-0.26)
                        .foregroundColor(PassmateColors.primaryDeep)
                }
            }
            Spacer()
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 16)
        .background(PassmateColors.surface)
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(PassmateColors.border, lineWidth: 1))
    }

    private var localLevel: HostLevel? {
        guard let level = profile.level else { return nil }

        return HostLevel.from(Int(level.level))
    }

    private var subtitle: String {
        var parts: [String] = []

        if let email = profile.email {
            parts.append(email)
        }
        if let joinedAt = profile.joinedAt {
            parts.append("\(String(joinedAt.prefix(10))) 가입")
        }
        if parts.isEmpty {
            return "구글 계정으로 로그인"
        } else {
            return parts.joined(separator: " · ")
        }
    }

    private func formatCoins(_ coins: Int64) -> String {
        let formatter = NumberFormatter()

        formatter.numberStyle = .decimal
        return formatter.string(from: NSNumber(value: coins)) ?? "\(coins)"
    }
}
