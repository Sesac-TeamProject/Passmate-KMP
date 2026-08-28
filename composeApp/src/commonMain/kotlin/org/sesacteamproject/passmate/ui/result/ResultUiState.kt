package org.sesacteamproject.passmate.ui.result

import org.sesacteamproject.passmate.rating.domain.model.RatingTag
import org.sesacteamproject.passmate.report.domain.model.LearningReport
import org.sesacteamproject.passmate.report.domain.model.SessionResult

data class ResultUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val result: SessionResult? = null,
    val report: LearningReport? = null,
    val selectedQuestionNo: Int? = null,
    val isSharing: Boolean = false,
    // 평가 시트 (T080) — canRate이고 미제출일 때만 노출
    val isRatingSheetVisible: Boolean = false,
    val ratingStars: Int = 0,
    val ratingTags: Set<RatingTag> = emptySet(),
    val ratingComment: String = "",
    val isSubmittingRating: Boolean = false,
    val hasRated: Boolean = false
)
