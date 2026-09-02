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
    // 평가 시트 (M-06 v2) — 세션 종료 후 제출 학생에게만, 결과 로드 시 자동으로 연다.
    // 자격 판정(제출 여부·24시간)은 서버의 canRate가 한다 (규칙 §1 서버 권위)
    val isRatingSheetVisible: Boolean = false,
    // 자동 표시는 화면 진입당 한 번만 — 리포트 갱신으로 재로드돼도 다시 열지 않는다
    val hasPromptedRating: Boolean = false,
    val ratingStars: Int = 0,
    val ratingTags: Set<RatingTag> = emptySet(),
    val ratingComment: String = "",
    val isSubmittingRating: Boolean = false,
    val hasRated: Boolean = false
)
