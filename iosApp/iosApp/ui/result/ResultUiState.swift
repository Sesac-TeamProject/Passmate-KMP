import Shared

struct ResultUiState {
    var isLoading: Bool = true

    var loadFailed: Bool = false

    var result: SessionResult?

    var report: LearningReport?

    var selectedQuestionNo: Int?

    var isSharing: Bool = false
}
