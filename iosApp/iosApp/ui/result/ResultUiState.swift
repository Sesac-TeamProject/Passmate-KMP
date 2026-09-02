import Shared

struct ResultUiState {
    var isLoading: Bool = true

    var loadFailed: Bool = false

    var result: SessionResult?

    var report: LearningReport?

    var selectedQuestionNo: Int?

    var isSharing: Bool = false

    // 평가 시트 (M-06 v2) — 세션 종료 후 제출 학생에게만, 결과 로드 시 자동으로 연다.
    // 자격 판정(제출 여부·24시간)은 서버의 canRate가 한다 (규칙 §1 서버 권위)
    var isRatingSheetVisible: Bool = false

    // 자동 표시는 화면 진입당 한 번만 — 리포트 갱신으로 재로드돼도 다시 열지 않는다
    var hasPromptedRating: Bool = false

    var ratingStars: Int = 0

    var ratingTags: Set<RatingTag> = []

    var ratingComment: String = ""

    var isSubmittingRating: Bool = false

    var hasRated: Bool = false
}
