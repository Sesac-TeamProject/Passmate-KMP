import Combine
import Foundation
import Shared

// Compose CreateRoomViewModel.kt 미러 — 세트 로드·방 생성 제출 (M-13 새 방 만들기 시트)
final class CreateRoomViewModel: ObservableObject {
    private let getMyQuestionSetsUseCase: GetMyQuestionSetsUseCase

    private let createRoomUseCase: CreateRoomUseCase

    @Published private(set) var uiState = CreateRoomUiState()

    let event = PassthroughSubject<CreateRoomEvent, Never>()

    private var hasEntered = false

    private func onEnter() {
        if hasEntered {
            return
        }
        hasEntered = true
        loadSets()
    }

    private func loadSets() {
        uiState.isLoadingSets = true
        uiState.setsLoadFailed = false
        // 방에는 확정(CONFIRMED) 세트만 연결 가능 — 검토 단계 강제 (FR-010)
        getMyQuestionSetsUseCase.invoke(confirmedOnly: true, cursor: nil) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                let page = (result as? AppResultSuccess<AnyObject>)?.value as? PagedResult

                if error == nil, let page {
                    let sets = page.items.compactMap { $0 as? QuestionSetSummary }

                    self.uiState.isLoadingSets = false
                    self.uiState.setsLoadFailed = false
                    self.uiState.sets = sets
                    if self.uiState.selectedSetId == nil {
                        self.uiState.selectedSetId = sets.first?.setId
                    }
                } else {
                    self.uiState.isLoadingSets = false
                    self.uiState.setsLoadFailed = true
                }
            }
        }
    }

    private func onSubmit() {
        let state = uiState

        if !state.canSubmit {
            return
        }
        uiState.isSubmitting = true
        createRoomUseCase.invoke(
            title: state.title,
            questionSetId: state.selectedSetId.map { KotlinLong(value: $0) },
            isPaid: state.isPaid,
            entryFee: Int(state.entryFeeText).map { KotlinInt(value: Int32($0)) }
        ) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                self.uiState.isSubmitting = false
                if error == nil, let created = (result as? AppResultSuccess<AnyObject>)?.value as? CreatedRoom {
                    self.event.send(.created(pin: created.pin))
                } else {
                    let appError = (result as? AppResultFailure)?.error

                    self.event.send(.showNotice(message: self.createFailMessage(appError)))
                }
            }
        }
    }

    // 서버 code 기반 문구 분기 (규칙 §10) — 최종 권위는 서버 검증
    private func createFailMessage(_ error: AppError?) -> String {
        if error?.serverCode == "HOST_LEVEL_REQUIRED" {
            return "유료 방은 Lv.3(검증된 운영자)부터 열 수 있어요"
        } else if let validation = error as? AppErrorValidationFailed {
            return validation.serverMessage ?? "입력값을 확인해 주세요"
        } else if error is AppErrorNetworkError {
            return "네트워크 연결을 확인해 주세요"
        } else {
            return "방을 만들지 못했어요. 다시 시도해 주세요"
        }
    }

    func action(_ action: CreateRoomAction) {
        switch action {
        case .enter:
            onEnter()
        case .retrySets:
            loadSets()
        case let .changeTitle(title):
            uiState.title = title
        case let .selectSet(setId):
            uiState.selectedSetId = setId
        case let .selectPaid(isPaid):
            uiState.isPaid = isPaid
        case let .changeEntryFee(text):
            uiState.entryFeeText = String(text.filter { $0.isNumber }.prefix(7))
        case .submit:
            onSubmit()
        }
    }

    init(
        getMyQuestionSetsUseCase: GetMyQuestionSetsUseCase,
        createRoomUseCase: CreateRoomUseCase
    ) {
        self.getMyQuestionSetsUseCase = getMyQuestionSetsUseCase
        self.createRoomUseCase = createRoomUseCase
    }
}
