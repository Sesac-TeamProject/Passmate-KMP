package org.sesacteamproject.passmate.rating.data.dto

import kotlinx.serialization.Serializable

// POST /rooms/{roomId}/ratings — {stars, tags[], comment?} (FR-042)
@Serializable
data class SubmitRatingRequest(
    val stars: Int,
    val tags: List<String>,
    val comment: String? = null
)
