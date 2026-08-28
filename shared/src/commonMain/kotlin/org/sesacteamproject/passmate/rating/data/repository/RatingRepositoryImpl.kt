package org.sesacteamproject.passmate.rating.data.repository

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.network.apiCall
import org.sesacteamproject.passmate.rating.data.dto.SubmitRatingRequest
import org.sesacteamproject.passmate.rating.data.remote.RatingRemoteDataSource
import org.sesacteamproject.passmate.rating.domain.model.RatingDraft
import org.sesacteamproject.passmate.rating.domain.repository.RatingRepository

class RatingRepositoryImpl(
    private val remoteDataSource: RatingRemoteDataSource
) : RatingRepository {

    override suspend fun submitRating(roomId: Long, draft: RatingDraft): AppResult<Unit> {
        val request = SubmitRatingRequest(
            stars = draft.stars,
            tags = draft.tags.map { it.wireValue },
            comment = draft.comment?.trim()?.ifEmpty { null }
        )

        return apiCall { remoteDataSource.submitRating(roomId, request) }
    }
}
