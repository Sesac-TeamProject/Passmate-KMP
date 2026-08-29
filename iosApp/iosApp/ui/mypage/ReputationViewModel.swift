import Combine
import Foundation
import Shared

// Compose ReputationViewModel.kt 미러 — 내 명성·뱃지 로드 (M-09)
final class ReputationViewModel: ObservableObject {
    private let getMyGradeUseCase: GetMyGradeUseCase

    private let getMyBadgesUseCase: GetMyBadgesUseCase

    private let isSignedInUseCase: IsSignedInUseCase

    @Published private(set) var uiState = ReputationUiState()

    let event = PassthroughSubject<ReputationEvent, Never>()

    private var hasEntered = false

    private func onEnter() {
        if hasEntered {
            return
        }
        hasEntered = true
        // 회원 전용 가드 — 서버 검증이 최종 권위지만 UX상 진입 시 먼저 로그인 유도 (규칙 §8)
        if isSignedInUseCase.invoke() {
            load()
        } else {
            event.send(.requireSignIn)
        }
    }

    private func load() {
        uiState.isLoading = true
        uiState.loadFailed = false
        getMyGradeUseCase.invoke { [weak self] gradeResult, gradeError in
            DispatchQueue.main.async {
                guard let self else { return }
                let grade = (gradeResult as? AppResultSuccess<AnyObject>)?.value as? MyGrade

                if gradeError == nil, let grade {
                    self.loadBadges(grade: grade)
                } else {
                    self.uiState.isLoading = false
                    self.uiState.loadFailed = true
                }
            }
        }
    }

    private func loadBadges(grade: MyGrade) {
        getMyBadgesUseCase.invoke { [weak self] badgesResult, badgesError in
            DispatchQueue.main.async {
                guard let self else { return }
                let badges = (badgesResult as? AppResultSuccess<AnyObject>)?.value as? [Badge]

                if badgesError == nil, let badges {
                    self.uiState.isLoading = false
                    self.uiState.loadFailed = false
                    self.uiState.grade = grade
                    self.uiState.badges = badges
                } else {
                    self.uiState.isLoading = false
                    self.uiState.loadFailed = true
                }
            }
        }
    }

    func action(_ action: ReputationAction) {
        switch action {
        case .enter:
            onEnter()
        case .retry:
            load()
        }
    }

    init(
        getMyGradeUseCase: GetMyGradeUseCase,
        getMyBadgesUseCase: GetMyBadgesUseCase,
        isSignedInUseCase: IsSignedInUseCase
    ) {
        self.getMyGradeUseCase = getMyGradeUseCase
        self.getMyBadgesUseCase = getMyBadgesUseCase
        self.isSignedInUseCase = isSignedInUseCase
    }
}
