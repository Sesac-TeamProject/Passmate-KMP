package org.sesacteamproject.passmate.question.data.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import org.sesacteamproject.passmate.question.data.dto.QuestionSetsResponse

// GET /question-sets — 계약 `PageResponse<QuestionSetSummaryResponse>` 기준.
// 커서가 아니라 page/size 응답이다.
class QuestionMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parsesServerPageAndMapsConfirmedFlag() {
        val raw = """
            {
              "content": [
                {"id":1,"title":"Spring 기초 세트","status":"CONFIRMED","source":"MANUAL","questionCount":8,"totalPoints":800,"estimatedSeconds":480},
                {"id":2,"title":"작성 중 세트","status":"DRAFT","source":"AI","questionCount":3,"totalPoints":300,"estimatedSeconds":180}
              ],
              "page": 0,
              "size": 20,
              "totalElements": 2,
              "totalPages": 1,
              "hasNext": false
            }
        """.trimIndent()

        val page = json.decodeFromString<QuestionSetsResponse>(raw).toDomain()

        assertEquals(2, page.items.size)
        // 서버 id → 도메인 setId
        assertEquals(1L, page.items.first().setId)
        assertEquals(true, page.items.first().isConfirmed)
        assertEquals(false, page.items.last().isConfirmed)
        assertEquals(8, page.items.first().questionCount)
        assertNull(page.nextCursor)
    }

    @Test
    fun carriesNextPageNumberAsCursor() {
        val raw = """{"content":[],"page":0,"size":20,"totalElements":40,"totalPages":2,"hasNext":true}"""

        val page = json.decodeFromString<QuestionSetsResponse>(raw).toDomain()

        assertEquals("1", page.nextCursor)
        assertEquals(true, page.hasNext)
    }
}
