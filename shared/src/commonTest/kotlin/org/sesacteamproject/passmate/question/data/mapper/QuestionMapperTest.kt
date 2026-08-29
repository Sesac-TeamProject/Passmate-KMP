package org.sesacteamproject.passmate.question.data.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import org.sesacteamproject.passmate.question.data.dto.QuestionSetsResponse

class QuestionMapperTest {

    @Test
    fun mapsSetsWithConfirmedFlag() {
        val response = QuestionSetsResponse(
            items = listOf(
                QuestionSetsResponse.QuestionSetDto(setId = 1, title = "Spring 기초 세트", status = "CONFIRMED", questionCount = 8),
                QuestionSetsResponse.QuestionSetDto(setId = 2, title = "작성 중 세트", status = "DRAFT", questionCount = 3)
            )
        )

        val page = response.toDomain()

        assertEquals(2, page.items.size)
        assertEquals(true, page.items.first().isConfirmed)
        assertEquals(false, page.items.last().isConfirmed)
        assertEquals(8, page.items.first().questionCount)
    }
}
