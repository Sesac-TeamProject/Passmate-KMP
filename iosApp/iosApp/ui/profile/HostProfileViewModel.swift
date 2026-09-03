import Combine
import Foundation
import Shared

// Compose HostProfileViewModel.kt 미러 — 호스트 공개 프로필 로드·차단·신고 (M-10)
final class HostProfileViewModel: ObservableObject {
    private let getHostProfileUseCase: GetHostProfileUseCase

    private let getRoomPinUseCase: GetRoomPinUseCase

    private let blockHostUseCase: BlockHostUseCase

    private let reportHostUseCase: ReportHostUseCase

    private let isSignedInUseCase: IsSignedInUseCase

    @Published private(set) var uiState = HostProfileUiState()

    let event = PassthroughSubject<HostProfileEvent, Never>()

    private var loadedHostId: Int64?

    // 목록에 pin이 없어 roomId로 조회해 Join 라우트로 넘긴다
    private func onClickRoom(roomId: Int64) {
        getRoomPinUseCase.invoke(roomId: roomId) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                let pin = (result as? AppResultSuccess<AnyObject>)?.value as? String

                if error == nil, let pin {
                    self.event.send(.joinRoom(pin: pin))
                } else {
                    self.event.send(.showNotice(message: "방 정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요"))
                }
            }
        }
    }

    private func onEnter(hostId: Int64) {
        if loadedHostId != hostId {
            load(hostId: hostId)
        }
    }

    private func load(hostId: Int64) {
        loadedHostId = hostId
        uiState = HostProfileUiState(isLoading: true)
        getHostProfileUseCase.invoke(userId: hostId) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                let profile = (result as? AppResultSuccess<AnyObject>)?.value as? HostProfile

                if error == nil, let profile {
                    self.uiState.isLoading = false
                    self.uiState.loadFailed = false
                    self.uiState.profile = profile
                } else {
                    self.uiState.isLoading = false
                    self.uiState.loadFailed = true
                }
            }
        }
    }

    private func onClickBlock() {
        guard let profile = uiState.profile, !uiState.isSubmitting else { return }
        // 차단은 회원 전용 — 게스트는 로그인 유도 (규칙 §8)
        if isSignedInUseCase.invoke() {
            uiState.isSubmitting = true
            blockHostUseCase.invoke(userId: profile.userId) { [weak self] result, error in
                DispatchQueue.main.async {
                    guard let self else { return }
                    self.uiState.isSubmitting = false
                    if error == nil, result is AppResultSuccess<AnyObject> {
                        self.event.send(.blockedAndClose)
                    } else {
                        self.event.send(.showNotice(message: "차단하지 못했어요. 다시 시도해 주세요"))
                    }
                }
            }
        } else {
            event.send(.requireSignIn)
        }
    }

    private func onSubmitReport(reason: ReportReason) {
        guard let profile = uiState.profile, !uiState.isSubmitting else { return }
        uiState.isSubmitting = true
        reportHostUseCase.invoke(userId: profile.userId, reason: reason, detail: nil) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                self.uiState.isSubmitting = false
                if error == nil, result is AppResultSuccess<AnyObject> {
                    self.uiState.isReported = true
                } else {
                    self.event.send(.showNotice(message: "신고를 접수하지 못했어요. 다시 시도해 주세요"))
                }
            }
        }
    }

    func action(_ action: HostProfileAction) {
        switch action {
        case let .enter(hostId):
            onEnter(hostId: hostId)
        case let .retry(hostId):
            load(hostId: hostId)
        case let .clickRoom(roomId):
            onClickRoom(roomId: roomId)
        case .clickBlock:
            onClickBlock()
        case let .submitReport(reason):
            onSubmitReport(reason: reason)
        }
    }

    init(
        getHostProfileUseCase: GetHostProfileUseCase,
        blockHostUseCase: BlockHostUseCase,
        reportHostUseCase: ReportHostUseCase,
        isSignedInUseCase: IsSignedInUseCase,
        getRoomPinUseCase: GetRoomPinUseCase
    ) {
        self.getHostProfileUseCase = getHostProfileUseCase
        self.blockHostUseCase = blockHostUseCase
        self.reportHostUseCase = reportHostUseCase
        self.isSignedInUseCase = isSignedInUseCase
        self.getRoomPinUseCase = getRoomPinUseCase
    }
}
