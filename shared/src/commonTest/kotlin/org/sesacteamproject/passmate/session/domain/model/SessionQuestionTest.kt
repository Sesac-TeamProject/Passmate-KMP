package org.sesacteamproject.passmate.session.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

// 서버는 OX 문항에 choices를 주지 않는다(실측: type=OX, choices=null, answer="O").
// 표시와 제출이 같은 목록을 봐야 눌린 보기와 보낸 값이 어긋나지 않는다.
class SessionQuestionTest {

    private fun question(type: QuestionType, choices: List<String>): SessionQuestion {
        return SessionQuestion(
            questionId = 1L,
            questionNo = 1,
            type = type,
            body = "지구는 둥글다",
            choices = choices,
            points = 100,
            timeLimitSec = 30,
            endsAt = "2026-09-04T12:00:00",
            isClosed = false
        )
    }

    @Test
    fun fillsOxChoicesWhenServerOmitsThem() {
        val target = question(QuestionType.OX, emptyList())

        assertEquals(listOf("O", "X"), target.answerChoices)
    }

    @Test
    fun keepsServerChoicesForOxWhenProvided() {
        val target = question(QuestionType.OX, listOf("참", "거짓"))

        assertEquals(listOf("참", "거짓"), target.answerChoices)
    }

    @Test
    fun leavesOtherTypesUntouched() {
        val target = question(QuestionType.MULTIPLE_CHOICE, listOf("1", "2", "3", "4"))

        assertEquals(listOf("1", "2", "3", "4"), target.answerChoices)
        assertEquals(emptyList<String>(), question(QuestionType.ESSAY, emptyList()).answerChoices)
    }
}
