package org.sesacteamproject.passmate.rating.domain.repository

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.rating.domain.model.RatingDraft

interface RatingRepository {

    // 세션당 1회, 재평가 409 ALREADY_RATED (FR-042~043)
    suspend fun submitRating(roomId: Long, draft: RatingDraft): AppResult<Unit>
}
