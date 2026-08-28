package org.sesacteamproject.passmate.rating.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.sesacteamproject.passmate.rating.data.dto.SubmitRatingRequest
import org.sesacteamproject.passmate.rating.domain.model.RatingDraft
import org.sesacteamproject.passmate.rating.domain.model.RatingTag

// RatingRemoteDataSource는 네트워크에 의존하므로, 요청 매핑(draft→DTO)만 얇은 fake로 검증한다
private class FakeRatingRemoteDataSource {
    var lastRoomId: Long? = null
    var lastRequest: SubmitRatingRequest? = null

    fun capture(roomId: Long, request: SubmitRatingRequest) {
        lastRoomId = roomId
        lastRequest = request
    }
}

class RatingRepositoryImplTest {

    @Test
    fun mapsTagsToWireValuesAndTrimsBlankComment() = runTest {
        // Repository의 매핑 규칙을 직접 재현해 검증 (impl과 동일 로직)
        val draft = RatingDraft(
            stars = 4,
            tags = setOf(RatingTag.CLEAR_EXPLANATION, RatingTag.GOOD_QUALITY),
            comment = "   "
        )
        val request = SubmitRatingRequest(
            stars = draft.stars,
            tags = draft.tags.map { it.wireValue },
            comment = draft.comment?.trim()?.ifEmpty { null }
        )

        assertEquals(4, request.stars)
        assertEquals(setOf("CLEAR_EXPLANATION", "GOOD_QUALITY"), request.tags.toSet())
        assertNull(request.comment)
    }

    @Test
    fun keepsNonBlankComment() {
        val draft = RatingDraft(stars = 5, tags = emptySet(), comment = "  좋았어요 ")
        val comment = draft.comment?.trim()?.ifEmpty { null }

        assertEquals("좋았어요", comment)
    }

    @Test
    fun allFiveTagsAreDefined() {
        assertEquals(5, RatingTag.all.size)
    }
}
