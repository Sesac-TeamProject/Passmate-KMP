import SwiftUI
import Shared

// 시트 4종 중 무엇이 열려 있는지 — 표시 여부는 이 화면이 소유한다 (규칙 §11-1)
private enum MyInfoSheet: Identifiable {
    case editProfile(nickname: String, avatarId: Int?)
    case paymentMethod
    case settlementAccount
    case notifications

    var id: String {
        switch self {
        case .editProfile: return "editProfile"
        case .paymentMethod: return "paymentMethod"
        case .settlementAccount: return "settlementAccount"
        case .notifications: return "notifications"
        }
    }
}

// Figma "UI 디자인 v6" M-12(349:9683) 미러 — 마이 탭 루트: 프로필·계정·코인·정산·알림·로그아웃
struct MyInfoView: View {
    var onRequireSignIn: () -> Void = {}

    var onOpenReputation: () -> Void = {}

    var onOpenCoinHistory: () -> Void = {}

    var onOpenEarnings: () -> Void = {}

    var onOpenSettings: () -> Void = {}

    var onSignedOut: () -> Void = {}

    @StateObject private var viewModel = MyInfoViewModel(
        getMyProfileUseCase: KoinHelper.shared.getMyProfileUseCase(),
        getMyCoinsUseCase: KoinHelper.shared.getMyCoinsUseCase(),
        getEarningsUseCase: KoinHelper.shared.getEarningsUseCase(),
        signOutUseCase: KoinHelper.shared.signOutUseCase(),
        isSignedInUseCase: KoinHelper.shared.isSignedInUseCase()
    )

    @State private var activeSheet: MyInfoSheet?

    @State private var showSignOutConfirm = false

    @State private var noticeMessage: String?

    var body: some View {
        MyInfoContentView(
            uiState: viewModel.uiState,
            onAction: { viewModel.action($0) },
            onClickSignOut: { showSignOutConfirm = true }
        )
        .onAppear {
            viewModel.action(.enter)
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .requireSignIn:
                onRequireSignIn()
            case .openReputation:
                onOpenReputation()
            case let .openEditProfile(nickname, avatarId):
                activeSheet = .editProfile(nickname: nickname, avatarId: avatarId)
            case .openPaymentMethod:
                activeSheet = .paymentMethod
            case .openCoinHistory:
                onOpenCoinHistory()
            case .openSettlementAccount:
                activeSheet = .settlementAccount
            case .openEarnings:
                onOpenEarnings()
            case .openNotifications:
                activeSheet = .notifications
            case .openSettings:
                onOpenSettings()
            case .signedOut:
                onSignedOut()
            case let .showNotice(message):
                noticeMessage = message
            }
        }
        .sheet(item: $activeSheet) { sheet in
            sheetContent(sheet)
                .presentationDetents([.medium, .large])
        }
        .alert("로그아웃 할까요?", isPresented: $showSignOutConfirm) {
            Button("로그아웃", role: .destructive) {
                viewModel.action(.confirmSignOut)
            }
            Button("취소", role: .cancel) {}
        } message: {
            Text("다시 로그인하면 기록과 코인은 그대로 있어요.")
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
                    viewModel.action(.paymentMethodUpdated)
                },
                onNotice: { viewModel.action(.notice(message: $0)) },
                onClose: { activeSheet = nil }
            )
        case .settlementAccount:
            SettlementAccountSheetView(
                onSaved: {
                    activeSheet = nil
                    viewModel.action(.accountUpdated)
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

    let onClickSignOut: () -> Void

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
                    Text("마이")
                        .font(.system(size: 24, weight: .bold))
                        .kerning(-0.48)
                        .foregroundColor(PassmateColors.textPrimary)
                    Spacer()
                    Button(action: { onAction(.clickSettings) }) {
                        Text("설정")
                            .font(.system(size: 14, weight: .medium))
                            .kerning(-0.28)
                            .foregroundColor(PassmateColors.textSecondary)
                    }
                }
                if let profile = uiState.profile {
                    ProfileCardView(profile: profile, onClick: { onAction(.clickProfile) })
                    sectionCard {
                        infoRow(title: "닉네임", subtitle: profile.nickname, actionLabel: "변경") { onAction(.clickEditProfile) }
                        rowDivider
                        infoRow(title: "내 캐릭터", subtitle: avatarName(profile), actionLabel: "변경") { onAction(.clickEditProfile) }
                    }
                    sectionCard {
                        coinRow(coins: profile.coins?.int64Value ?? 0) { onAction(.clickCharge) }
                        rowDivider
                        infoRow(title: "결제 수단", subtitle: paymentMethodSubtitle, actionLabel: "관리") { onAction(.clickPaymentMethod) }
                        rowDivider
                        infoRow(title: "코인 내역", subtitle: recentTransactionSubtitle, actionLabel: "보기") { onAction(.clickCoinHistory) }
                    }
                    sectionCard {
                        infoRow(title: "정산 계좌", subtitle: settlementAccountSubtitle, actionLabel: "변경") { onAction(.clickSettlementAccount) }
                        rowDivider
                        infoRow(title: "이번 달 정산 예정", subtitle: nextPayoutSubtitle, actionLabel: "내역") { onAction(.clickEarnings) }
                    }
                    sectionCard {
                        infoRow(title: "알림", subtitle: "세션 시작 · 별점 요청 · 정산", actionLabel: "설정") { onAction(.clickNotifications) }
                    }
                }
                signOutButton
            }
            .padding(.horizontal, 20)
            .padding(.top, 32)
            .padding(.bottom, 24)
        }
    }

    private var rowDivider: some View {
        Rectangle()
            .fill(PassmateColors.border)
            .frame(height: 1)
    }

    private var signOutButton: some View {
        Button(action: onClickSignOut) {
            HStack {
                Spacer()
                if uiState.isProcessing {
                    ProgressView().tint(PassmateColors.primary)
                } else {
                    Text("로그아웃")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(PassmateColors.textSecondary)
                }
                Spacer()
            }
            .padding(.vertical, 16)
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(PassmateColors.border, lineWidth: 1))
        }
        .disabled(uiState.isProcessing)
        .padding(.top, 10)
    }

    private var paymentMethodSubtitle: String {
        if uiState.isCoinInfoFailed {
            return "불러오지 못했어요"
        } else if let method = uiState.defaultMethod {
            return "\(method.label) · 포트원 안전결제"
        } else {
            return "기본 결제 수단을 설정해 주세요"
        }
    }

    private var recentTransactionSubtitle: String {
        if uiState.isCoinInfoFailed {
            return "불러오지 못했어요"
        } else if let recent = uiState.recentTransaction {
            return "최근 \(shortDate(recent.createdAt)) \(signedCoins(Int(recent.amount))) C"
        } else {
            return "아직 내역이 없어요"
        }
    }

    private var settlementAccountSubtitle: String {
        if uiState.isEarningsFailed {
            return "불러오지 못했어요"
        } else if let account = uiState.settlementAccount {
            return "\(account.bankName) \(account.maskedNumber)"
        } else {
            return "계좌를 등록해 주세요"
        }
    }

    private var nextPayoutSubtitle: String {
        if uiState.isEarningsFailed {
            return "불러오지 못했어요"
        } else if let payout = uiState.nextPayout {
            return "₩\(formatNumber(payout.amount)) · \(payout.dateLabel) 지급"
        } else {
            return "정산 예정 없음"
        }
    }

    private func avatarName(_ profile: UserProfile) -> String {
        return StudentAvatars.nameOf(profile.avatarId.map { Int(truncating: $0) } ?? StudentAvatars.defaultId)
    }

    private func sectionCard<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        VStack(spacing: 0) {
            content()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 4)
        .background(PassmateColors.surface)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(PassmateColors.border, lineWidth: 1))
    }

    private func infoRow(title: String, subtitle: String, actionLabel: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.system(size: 15, weight: .medium))
                        .kerning(-0.3)
                        .foregroundColor(PassmateColors.textPrimary)
                    Text(subtitle)
                        .font(.system(size: 13))
                        .kerning(-0.26)
                        .foregroundColor(PassmateColors.textSecondary)
                }
                Spacer()
                Text("\(actionLabel) ›")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.primaryDeep)
            }
            .padding(.vertical, 14)
        }
    }

    private func coinRow(coins: Int64, onClickCharge: @escaping () -> Void) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("보유 코인")
                    .font(.system(size: 15, weight: .medium))
                    .kerning(-0.3)
                    .foregroundColor(PassmateColors.textPrimary)
                Text("\(formatNumber(coins)) C · 유료 방 참가비에 사용")
                    .font(.system(size: 13))
                    .kerning(-0.26)
                    .foregroundColor(PassmateColors.textSecondary)
            }
            Spacer()
            Button(action: onClickCharge) {
                Text("코인 충전")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(PassmateColors.surface)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(PassmateColors.primary)
                    .cornerRadius(12)
            }
        }
        .padding(.vertical, 14)
    }

    // "2026-08-22T10:00:00Z" → "8/22"
    private func shortDate(_ raw: String?) -> String {
        guard let raw else { return "" }
        let parts = raw.prefix(10).split(separator: "-")
        if parts.count == 3 {
            let month = Int(parts[1]).map(String.init) ?? String(parts[1])
            let day = Int(parts[2]).map(String.init) ?? String(parts[2])
            return "\(month)/\(day)"
        } else {
            return String(raw.prefix(10))
        }
    }

    private func signedCoins(_ amount: Int) -> String {
        if amount > 0 {
            return "+\(formatNumber(Int64(amount)))"
        } else {
            return "-\(formatNumber(Int64(-amount)))"
        }
    }

    private func formatNumber(_ value: Int64) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        return formatter.string(from: NSNumber(value: value)) ?? "\(value)"
    }
}

private struct ProfileCardView: View {
    let profile: UserProfile

    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
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
                    Text("참여한 방 \(profile.joinedRoomCount?.intValue ?? 0) · 내가 만든 방 \(profile.hostedRoomCount?.intValue ?? 0)")
                        .font(.system(size: 13))
                        .kerning(-0.26)
                        .foregroundColor(PassmateColors.textSecondary)
                }
                Spacer()
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 16)
            .background(PassmateColors.surface)
            .overlay(RoundedRectangle(cornerRadius: 20).stroke(PassmateColors.border, lineWidth: 1))
        }
    }

    private var localLevel: HostLevel? {
        guard let level = profile.level else { return nil }
        return HostLevel.from(Int(level.level))
    }
}

// MARK: - 프리뷰 (Figma 시안 비교용)
#Preview("마이 — 로드 완료") {
    MyInfoContentView(
        uiState: MyInfoUiState(
            isLoading: false,
            profile: UserProfile(
                nickname: "준영",
                email: "junyoung@example.com",
                joinedAt: "2026-08-01",
                avatarId: KotlinInt(int: 1),
                level: Shared.HostLevel.growing,
                coins: KotlinLong(value: 1200),
                joinedRoomCount: KotlinInt(int: 32),
                hostedRoomCount: KotlinInt(int: 12)
            ),
            defaultMethod: PaymentMethod.kakaoPay,
            settlementAccount: SettlementAccountSummary(bankName: "국민", maskedNumber: "***-***-4821", payoutNote: nil),
            nextPayout: NextPayout(dateLabel: "9/5", amount: 64000)
        ),
        onAction: { _ in },
        onClickSignOut: {}
    )
}

#Preview("마이 — 코인·정산 실패") {
    MyInfoContentView(
        uiState: MyInfoUiState(
            isLoading: false,
            profile: UserProfile(
                nickname: "준영", email: nil, joinedAt: nil, avatarId: KotlinInt(int: 1),
                level: nil, coins: nil, joinedRoomCount: nil, hostedRoomCount: nil
            ),
            isCoinInfoFailed: true,
            isEarningsFailed: true
        ),
        onAction: { _ in },
        onClickSignOut: {}
    )
}
