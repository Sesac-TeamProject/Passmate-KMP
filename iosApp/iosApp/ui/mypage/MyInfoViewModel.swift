import Combine
import Foundation
import Shared

// 약관 전용 화면·계약이 아직 없다 — 라우트가 생기기 전까지는 안내 문구만 노출한다
private let termsNotice = "약관 · 개인정보 처리방침은 준비 중이에요"

// Compose MyInfoViewModel.kt 미러 — 마이 탭 루트 (M-12): 프로필·코인·정산 3섹션 독립 로드
final class MyInfoViewModel: ObservableObject {
    private let getMyProfileUseCase: GetMyProfileUseCase

    private let getMyCoinsUseCase: GetMyCoinsUseCase

    private let getEarningsUseCase: GetEarningsUseCase

    private let signOutUseCase: SignOutUseCase

    private let isSignedInUseCase: IsSignedInUseCase

    @Published private(set) var uiState = MyInfoUiState()

    let event = PassthroughSubject<MyInfoEvent, Never>()

    private var hasEntered = false

    private func onEnter() {
        if hasEntered {
            return
        }
        hasEntered = true
        // 회원 전용 가드 — 서버 검증이 최종 권위 (규칙 §8)
        if isSignedInUseCase.invoke() {
            loadAll()
        } else {
            event.send(.requireSignIn)
        }
    }

    private func loadAll() {
        loadProfile()
        loadCoinInfo()
        loadEarnings()
    }

    private func loadProfile() {
        uiState.isLoading = true
        uiState.loadFailed = false
        getMyProfileUseCase.invoke { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                let profile = (result as? AppResultSuccess<AnyObject>)?.value as? UserProfile
                self.uiState.isLoading = false
                if error == nil, let profile {
                    self.uiState.loadFailed = false
                    self.uiState.profile = profile
                } else {
                    self.uiState.loadFailed = true
                }
            }
        }
    }

    private func loadCoinInfo() {
        getMyCoinsUseCase.invoke { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                let coins = (result as? AppResultSuccess<AnyObject>)?.value as? CoinBalance
                if error == nil, let coins {
                    self.uiState.defaultMethod = coins.defaultMethod
                    self.uiState.recentTransaction = coins.recent
                    self.uiState.isCoinInfoFailed = false
                } else {
                    self.uiState.isCoinInfoFailed = true
                }
            }
        }
    }

    private func loadEarnings() {
        getEarningsUseCase.invoke(cursor: nil) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                let earnings = (result as? AppResultSuccess<AnyObject>)?.value as? Earnings
                if error == nil, let earnings {
                    self.uiState.settlementAccount = earnings.account
                    self.uiState.nextPayout = earnings.nextPayout
                    self.uiState.isEarningsFailed = false
                } else {
                    self.uiState.isEarningsFailed = true
                }
            }
        }
    }

    private func onClickEditProfile() {
        if let profile = uiState.profile {
            event.send(.openEditProfile(
                nickname: profile.nickname,
                avatarId: profile.avatarId.map { Int(truncating: $0) }
            ))
        }
    }

    private func onConfirmSignOut() {
        if uiState.isProcessing {
            return
        }
        uiState.isProcessing = true
        // 로컬 세션 정리는 shared가 항상 수행 — 실패 케이스 없음 (M-12-11)
        signOutUseCase.invoke { [weak self] _, _ in
            DispatchQueue.main.async {
                guard let self else { return }
                self.uiState.isProcessing = false
                self.event.send(.signedOut)
            }
        }
    }

    func action(_ action: MyInfoAction) {
        switch action {
        case .enter:
            onEnter()
        case .retry:
            loadAll()
        case .retryCoinInfo:
            loadCoinInfo()
        case .retryEarnings:
            loadEarnings()
        case .clickProfile:
            event.send(.openReputation)
        case .clickEditProfile:
            onClickEditProfile()
        case .clickCharge:
            event.send(.openCharge)
        case .clickPaymentMethod:
            event.send(.openPaymentMethod)
        case .clickCoinHistory:
            event.send(.openCoinHistory)
        case .clickSettlementAccount:
            event.send(.openSettlementAccount)
        case .clickEarnings:
            event.send(.openEarnings)
        case .clickNotifications:
            event.send(.openNotifications)
        case .clickDeleteAccount:
            event.send(.openDeleteAccount)
        case .clickTerms:
            event.send(.showNotice(message: termsNotice))
        case .confirmSignOut:
            onConfirmSignOut()
        case .profileUpdated:
            loadProfile()
            event.send(.showNotice(message: "내 정보를 저장했어요"))
        case .paymentMethodUpdated:
            loadCoinInfo()
            event.send(.showNotice(message: "기본 결제 수단을 저장했어요"))
        case .accountUpdated:
            loadEarnings()
            event.send(.showNotice(message: "정산 계좌를 저장했어요"))
        case let .notice(message):
            event.send(.showNotice(message: message))
        }
    }

    init(
        getMyProfileUseCase: GetMyProfileUseCase,
        getMyCoinsUseCase: GetMyCoinsUseCase,
        getEarningsUseCase: GetEarningsUseCase,
        signOutUseCase: SignOutUseCase,
        isSignedInUseCase: IsSignedInUseCase
    ) {
        self.getMyProfileUseCase = getMyProfileUseCase
        self.getMyCoinsUseCase = getMyCoinsUseCase
        self.getEarningsUseCase = getEarningsUseCase
        self.signOutUseCase = signOutUseCase
        self.isSignedInUseCase = isSignedInUseCase
    }
}
