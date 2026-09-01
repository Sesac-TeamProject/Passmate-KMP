package org.sesacteamproject.passmate.ui.result

import org.sesacteamproject.passmate.rating.domain.model.RatingTag

sealed interface ResultAction {

    data class Enter(val roomId: Long) : ResultAction

    data class SelectQuestion(val questionNo: Int) : ResultAction

    data object ClickExport : ResultAction

    data object ClickSignup : ResultAction

    data object Retry : ResultAction

    // 불러오기 실패(M-05e) 하단 문의 버튼 — 문의 채널이 계약에 없어 안내만 한다
    // 평가 시트 (T080)
    data object OpenRatingSheet : ResultAction

    data object DismissRatingSheet : ResultAction

    data class SelectRatingStars(val stars: Int) : ResultAction

    data class ToggleRatingTag(val tag: RatingTag) : ResultAction

    data class ChangeRatingComment(val comment: String) : ResultAction

    data object SubmitRating : ResultAction

    data object SkipRating : ResultAction
}
