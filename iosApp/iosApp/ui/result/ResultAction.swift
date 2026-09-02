import Shared

enum ResultAction {
    case enter(roomId: Int64)
    case selectQuestion(questionNo: Int)
    case clickExport
    case clickSignup
    case retry
    // 불러오기 실패(M-05e) 하단 문의 버튼 — 문의 채널이 계약에 없어 안내만 한다
    // 평가 시트 (T080)
    case openRatingSheet
    case dismissRatingSheet
    case selectRatingStars(stars: Int)
    case toggleRatingTag(tag: RatingTag)
    case changeRatingComment(comment: String)
    case submitRating
    case skipRating
}
