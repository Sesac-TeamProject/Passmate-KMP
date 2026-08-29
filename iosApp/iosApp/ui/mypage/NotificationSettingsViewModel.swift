import Combine
import Foundation
import Shared

// Compose NotificationSettingsViewModel.kt 미러 — 알림 설정 3종, 토글 즉시 저장 (M-12-10)
final class NotificationSettingsViewModel: ObservableObject {
    private let getNotificationSettingsUseCase: GetNotificationSettingsUseCase

    private let updateNotificationSettingsUseCase: UpdateNotificationSettingsUseCase

    @Published private(set) var uiState = NotificationSettingsUiState()

    let event = PassthroughSubject<NotificationSettingsEvent, Never>()

    private var hasEntered = false

    private func onEnter() {
        if hasEntered {
            return
        }
        hasEntered = true
        load()
    }

    private func load() {
        uiState.isLoading = true
        uiState.loadFailed = false
        getNotificationSettingsUseCase.invoke { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                let settings = (result as? AppResultSuccess<AnyObject>)?.value as? NotificationSettings

                if error == nil, let settings {
                    self.uiState.isLoading = false
                    self.uiState.loadFailed = false
                    self.uiState.sessionStart = settings.sessionStart
                    self.uiState.ratingRequest = settings.ratingRequest
                    self.uiState.settlementDone = settings.settlementDone
                } else {
                    self.uiState.isLoading = false
                    self.uiState.loadFailed = true
                }
            }
        }
    }

    // 토글 즉시 저장 — 낙관 반영 후 실패 시 원복한다 (M-12-10)
    private func onToggle(kind: NotificationKind) {
        if uiState.isSaving || uiState.isLoading {
            return
        }
        let before = uiState

        switch kind {
        case .sessionStart:
            uiState.sessionStart.toggle()
        case .ratingRequest:
            uiState.ratingRequest.toggle()
        case .settlementDone:
            uiState.settlementDone.toggle()
        }
        uiState.isSaving = true

        let settings = NotificationSettings(
            sessionStart: uiState.sessionStart,
            ratingRequest: uiState.ratingRequest,
            settlementDone: uiState.settlementDone
        )

        updateNotificationSettingsUseCase.invoke(settings: settings) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                if error == nil, result is AppResultSuccess<AnyObject> {
                    self.uiState.isSaving = false
                } else {
                    self.uiState = before
                    self.uiState.isSaving = false
                    self.event.send(.showNotice(message: "설정을 저장하지 못했어요"))
                }
            }
        }
    }

    func action(_ action: NotificationSettingsAction) {
        switch action {
        case .enter:
            onEnter()
        case .retry:
            load()
        case let .toggle(kind):
            onToggle(kind: kind)
        }
    }

    init(
        getNotificationSettingsUseCase: GetNotificationSettingsUseCase,
        updateNotificationSettingsUseCase: UpdateNotificationSettingsUseCase
    ) {
        self.getNotificationSettingsUseCase = getNotificationSettingsUseCase
        self.updateNotificationSettingsUseCase = updateNotificationSettingsUseCase
    }
}
