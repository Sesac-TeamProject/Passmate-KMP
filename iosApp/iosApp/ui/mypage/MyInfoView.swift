import SwiftUI
import Shared

// Figma "UI 디자인 v6" M-12(349:9683) 미러 — 마이 탭 루트: 프로필·계정·코인·정산·알림·로그아웃
struct MyInfoView: View {
    var onRequireSignIn: () -> Void = {}

    var onOpenReputation: () -> Void = {}

    var onOpenCoinHistory: () -> Void = {}

    var onOpenCharge: () -> Void = {}

    var onOpenEarnings: () -> Void = {}

    var onOpenDeleteAccount: () -> Void = {}

    var onOpenEditProfile: () -> Void = {}

    var onOpenPaymentMethod: () -> Void = {}

    var onOpenSettlementAccount: () -> Void = {}

    var onOpenNotifications: () -> Void = {}

    var onSignedOut: () -> Void = {}

    @StateObject private var viewModel = MyInfoViewModel(
        getMyProfileUseCase: KoinHelper.shared.getMyProfileUseCase(),
        getMyCoinsUseCase: KoinHelper.shared.getMyCoinsUseCase(),
        getEarningsUseCase: KoinHelper.shared.getEarningsUseCase(),
        signOutUseCase: KoinHelper.shared.signOutUseCase(),
        isSignedInUseCase: KoinHelper.shared.isSignedInUseCase()
    )

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
            case .openEditProfile:
                onOpenEditProfile()
            case .openPaymentMethod:
                onOpenPaymentMethod()
            case .openCoinHistory:
                onOpenCoinHistory()
            case .openCharge:
                onOpenCharge()
            case .openSettlementAccount:
                onOpenSettlementAccount()
            case .openEarnings:
                onOpenEarnings()
            case .openNotifications:
                onOpenNotifications()
            case .openDeleteAccount:
                onOpenDeleteAccount()
            case .signedOut:
                onSignedOut()
            case let .showNotice(message):
                noticeMessage = message
            }
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

// in-flight 스피너 치수 — 행(로그아웃)과 실패 카드(다시 시도)가 같은 크기를 쓴다. Compose RowSpec 미러
private enum RowSpec {
    static let spinnerScale: CGFloat = 0.8
}

// 부분 실패 문구 (시안 M-12e) — Compose MyInfoScreen.kt의 FailureText 미러
private enum FailureText {
    static let banner = "일부 정보를 불러오지 못했어요 · 아래에서 다시 시도"

    static let coinCard = "코인 정보를 불러오지 못했어요"

    static let earningsCard = "정산 정보를 불러오지 못했어요"

    static let retry = "다시 시도"
}

// 부분 실패 치수·타이포 (시안 M-12e) — Compose FailureSpec 미러
private enum FailureSpec {
    static let bannerHeight: CGFloat = 44

    static let bannerCornerRadius: CGFloat = 12

    static let bannerFontSize: CGFloat = 13

    static let bannerKerning: CGFloat = -0.13

    static let cardHeight: CGFloat = 150

    static let cardCornerRadius: CGFloat = 16

    static let cardBorderWidth: CGFloat = 1

    static let iconSize: CGFloat = 22

    static let messageTopPadding: CGFloat = 10

    static let messageFontSize: CGFloat = 14

    static let messageKerning: CGFloat = -0.14

    static let retryTopPadding: CGFloat = 2

    static let retryFontSize: CGFloat = 13

    static let retryKerning: CGFloat = -0.13

    static let retryTouchPadding: CGFloat = 8
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
                }
                if uiState.hasPartialFailure {
                    partialFailureBanner
                }
                if let profile = uiState.profile {
                    ProfileCardView(profile: profile, onClick: { onAction(.clickProfile) })
                    sectionCard {
                        infoRow(title: "닉네임", subtitle: profile.nickname, actionLabel: "변경") { onAction(.clickEditProfile) }
                        rowDivider
                        infoRow(title: "내 캐릭터", subtitle: avatarName(profile), actionLabel: "변경") { onAction(.clickEditProfile) }
                    }
                    // 코인·정산은 카드 단위로만 실패시킨다 — 프로필이 정상이면 화면 전체를 덮지 않는다 (규칙 §9)
                    if uiState.isCoinInfoFailed {
                        failureCard(message: FailureText.coinCard, isRetrying: uiState.isCoinInfoLoading) { onAction(.retryCoinInfo) }
                    } else {
                        sectionCard {
                            coinRow(coins: profile.coins?.int64Value ?? 0) { onAction(.clickCharge) }
                            rowDivider
                            infoRow(title: "결제 수단", subtitle: paymentMethodSubtitle, actionLabel: "관리") { onAction(.clickPaymentMethod) }
                            rowDivider
                            infoRow(title: "코인 내역", subtitle: recentTransactionSubtitle, actionLabel: "보기") { onAction(.clickCoinHistory) }
                        }
                    }
                    if uiState.isEarningsFailed {
                        failureCard(message: FailureText.earningsCard, isRetrying: uiState.isEarningsLoading) { onAction(.retryEarnings) }
                    } else {
                        sectionCard {
                            infoRow(title: "정산 계좌", subtitle: settlementAccountSubtitle, actionLabel: "변경") { onAction(.clickSettlementAccount) }
                            rowDivider
                            infoRow(title: "이번 달 정산 예정", subtitle: nextPayoutSubtitle, actionLabel: "내역") { onAction(.clickEarnings) }
                        }
                    }
                    sectionCard {
                        infoRow(title: "알림 설정", subtitle: "세션 시작 · 별점 요청 · 정산", actionLabel: "변경") { onAction(.clickNotifications) }
                        rowDivider
                        // 확인 알림을 거쳐야 실제 로그아웃 — 알림 소유는 상위 View (규칙 §11-1)
                        infoRow(
                            title: "로그아웃",
                            subtitle: nil,
                            actionLabel: "",
                            actionColor: PassmateColors.destructive,
                            isProcessing: uiState.isProcessing
                        ) {
                            onClickSignOut()
                        }
                        rowDivider
                        infoRow(title: "회원 탈퇴", subtitle: nil, actionLabel: "", actionColor: PassmateColors.destructive) {
                            onAction(.clickDeleteAccount)
                        }
                        rowDivider
                        infoRow(title: "약관 · 개인정보 처리방침", subtitle: "버전 1.0.0", actionLabel: "보기") { onAction(.clickTerms) }
                    }
                }
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

    // 카드 하나라도 실패했을 때의 상단 안내 (시안 M-12e banner/부분 실패)
    private var partialFailureBanner: some View {
        Text(FailureText.banner)
            .font(.system(size: FailureSpec.bannerFontSize, weight: .medium))
            .kerning(FailureSpec.bannerKerning)
            .foregroundColor(PassmateColors.wrongPinkText)
            .frame(maxWidth: .infinity)
            .frame(height: FailureSpec.bannerHeight)
            .background(PassmateColors.errorIconBg)
            .cornerRadius(FailureSpec.bannerCornerRadius)
    }

    // 카드 단위 실패 자리표시자 (시안 M-12e card/실패) — 해당 섹션만 다시 불러온다
    private func failureCard(message: String, isRetrying: Bool, onRetry: @escaping () -> Void) -> some View {
        VStack(spacing: 0) {
            PassmateIconView(icon: .alertCircle, tint: PassmateColors.textTertiary, size: FailureSpec.iconSize)
            Text(message)
                .font(.system(size: FailureSpec.messageFontSize, weight: .medium))
                .kerning(FailureSpec.messageKerning)
                .foregroundColor(PassmateColors.textPrimary)
                .padding(.top, FailureSpec.messageTopPadding)
            if isRetrying {
                ProgressView()
                    .scaleEffect(RowSpec.spinnerScale)
                    .tint(PassmateColors.primary)
                    .padding(FailureSpec.retryTouchPadding)
                    .padding(.top, FailureSpec.retryTopPadding)
            } else {
                Button(action: onRetry) {
                    Text(FailureText.retry)
                        .font(.system(size: FailureSpec.retryFontSize, weight: .bold))
                        .kerning(FailureSpec.retryKerning)
                        .foregroundColor(PassmateColors.primaryDeep)
                        .padding(FailureSpec.retryTouchPadding)
                }
                .padding(.top, FailureSpec.retryTopPadding)
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: FailureSpec.cardHeight)
        .background(PassmateColors.surface)
        .overlay(
            RoundedRectangle(cornerRadius: FailureSpec.cardCornerRadius)
                .stroke(PassmateColors.border, lineWidth: FailureSpec.cardBorderWidth)
        )
    }

    // 실패는 카드 자체가 failureCard로 대체되므로 여기서는 성공·빈 값만 다룬다
    private var paymentMethodSubtitle: String {
        if let method = uiState.defaultMethod {
            return "\(method.label) · 포트원 안전결제"
        } else {
            return "기본 결제 수단을 설정해 주세요"
        }
    }

    private var recentTransactionSubtitle: String {
        if let recent = uiState.recentTransaction {
            return "최근 \(shortDate(recent.createdAt)) \(signedCoins(Int(recent.amount))) C"
        } else {
            return "아직 내역이 없어요"
        }
    }

    private var settlementAccountSubtitle: String {
        if let account = uiState.settlementAccount {
            return "\(account.bankName) \(account.maskedNumber)"
        } else {
            return "계좌를 등록해 주세요"
        }
    }

    private var nextPayoutSubtitle: String {
        if let payout = uiState.nextPayout {
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

    private func infoRow(
        title: String,
        subtitle: String?,
        actionLabel: String,
        actionColor: Color = PassmateColors.primaryDeep,
        isProcessing: Bool = false,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.system(size: 15, weight: .medium))
                        .kerning(-0.3)
                        .foregroundColor(PassmateColors.textPrimary)
                    if let subtitle {
                        Text(subtitle)
                            .font(.system(size: 13))
                            .kerning(-0.26)
                            .foregroundColor(PassmateColors.textSecondary)
                    }
                }
                Spacer()
                if isProcessing {
                    ProgressView()
                        .scaleEffect(RowSpec.spinnerScale)
                        .tint(PassmateColors.primary)
                } else {
                    Text(actionLabel.isEmpty ? "›" : "\(actionLabel) ›")
                        .font(.system(size: 14, weight: .medium))
                        .kerning(-0.28)
                        .foregroundColor(actionColor)
                }
            }
            .padding(.vertical, 14)
        }
        .disabled(isProcessing)
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
