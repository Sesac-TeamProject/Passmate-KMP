import Shared

enum ResultAction {
    case enter(roomId: Int64)
    case selectQuestion(questionNo: Int)
    case clickExport
    case clickSignup
    case retry
    // 평가 시트 (T080)
    case openRatingSheet
    case dismissRatingSheet
    case selectRatingStars(stars: Int)
    case toggleRatingTag(tag: RatingTag)
    case changeRatingComment(comment: String)
    case submitRating
    case skipRating
}
