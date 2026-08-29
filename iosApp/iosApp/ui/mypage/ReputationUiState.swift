import Shared

struct ReputationUiState {
    var isLoading: Bool = true

    var loadFailed: Bool = false

    var grade: MyGrade?

    var badges: [Badge] = []
}
