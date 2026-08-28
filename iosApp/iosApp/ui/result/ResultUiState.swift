import Shared

struct ResultUiState {
    var isLoading: Bool = true

    var loadFailed: Bool = false

    var result: SessionResult?

    var report: LearningReport?

    var selectedQuestionNo: Int?

    var isSharing: Bool = false

    // 평가 시트 (T080)
    var isRatingSheetVisible: Bool = false

    var ratingStars: Int = 0

    var ratingTags: Set<RatingTag> = []

    var ratingComment: String = ""

    var isSubmittingRating: Bool = false

    var hasRated: Bool = false
}
