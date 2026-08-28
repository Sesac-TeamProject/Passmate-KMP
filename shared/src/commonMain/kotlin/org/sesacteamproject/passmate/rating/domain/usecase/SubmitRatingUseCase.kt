package org.sesacteamproject.passmate.rating.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.rating.domain.model.RatingDraft
import org.sesacteamproject.passmate.rating.domain.repository.RatingRepository

class SubmitRatingUseCase(
    private val ratingRepository: RatingRepository
) {
    suspend operator fun invoke(roomId: Long, draft: RatingDraft): AppResult<Unit> {
        return ratingRepository.submitRating(roomId, draft)
    }
}
