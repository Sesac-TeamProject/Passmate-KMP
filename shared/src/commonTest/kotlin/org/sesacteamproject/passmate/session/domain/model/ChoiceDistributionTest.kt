package org.sesacteamproject.passmate.session.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// M-04 응답 분포 — 서버 키를 보기 목록에 짝짓는 규칙.
// 키가 보기 원문이라는 것은 로컬 실서버에서 확인했다 (2026-09-04, room 31 / question 4):
//   {"answer":"2","distribution":{"1":1,"2":1}}
class ChoiceDistributionTest {

    private fun mcq(): SessionQuestion {
        return SessionQuestion(
            questionId = 4,
            questionNo = 1,
            type = QuestionType.MULTIPLE_CHOICE,
            body = "1+1은?",
            choices = listOf("1", "2", "3", "4"),
            points = 100,
            timeLimitSec = 30,
            endsAt = "2026-09-04T10:00:00Z",
            isClosed = true
        )
    }

    private fun ox(): SessionQuestion {
        return SessionQuestion(
            questionId = 5,
            questionNo = 2,
            type = QuestionType.OX,
            body = "지구는 둥글다",
            // 서버는 OX에 choices를 주지 않는다 — answerChoices가 O/X를 채운다
            choices = emptyList(),
            points = 100,
            timeLimitSec = 30,
            endsAt = "2026-09-04T10:00:00Z",
            isClosed = true
        )
    }

    // 키는 학생이 낸 문자열 그대로다. 보기 원문과 문자열로 짝지어진다
    @Test
    fun matchesServerKeysToChoiceLabels() {
        val rows = mcq().distributionOf(
            raw = mapOf("1" to 1, "2" to 1),
            answer = "2",
            myChoiceIndex = 1
        )

        assertEquals(listOf("1", "2", "3", "4"), rows.map { it.label })
        assertEquals(listOf(1, 2, 3, 4), rows.map { it.choiceNo })
        assertEquals(listOf(1, 1, 0, 0), rows.map { it.count })
    }

    // 아무도 안 고른 보기는 서버 응답에 키가 없다 — 빠뜨리지 않고 0명으로 그린다 (시안 M-04 4번 보기)
    @Test
    fun keepsUnansweredChoicesAsZero() {
        val rows = mcq().distributionOf(
            raw = mapOf("2" to 3),
            answer = "2",
            myChoiceIndex = null
        )

        assertEquals(4, rows.size)
        assertEquals(0, rows.last().count)
    }

    @Test
    fun marksAnswerAndMyChoice() {
        val rows = mcq().distributionOf(
            raw = mapOf("1" to 1, "2" to 1),
            answer = "2",
            myChoiceIndex = 0
        )

        assertTrue(rows[1].isAnswer)
        assertFalse(rows[0].isAnswer)
        assertTrue(rows[0].isMine)
        assertFalse(rows[1].isMine)
    }

    // OX는 서버가 choices를 안 줘서 answerChoices의 O/X와 짝지어야 한다.
    // 여기서 어긋나면 OX 문항의 분포가 통째로 0명으로 그려진다
    @Test
    fun matchesOxChoicesFromDomainDefaults() {
        val rows = ox().distributionOf(
            raw = mapOf("O" to 5, "X" to 2),
            answer = "O",
            myChoiceIndex = 1
        )

        assertEquals(listOf("O", "X"), rows.map { it.label })
        assertEquals(listOf(5, 2), rows.map { it.count })
        assertTrue(rows[0].isAnswer)
        assertTrue(rows[1].isMine)
    }

    // 서술형은 보기가 없다 — 행을 만들지 않아 화면이 분포 섹션을 통째로 숨긴다
    @Test
    fun returnsEmptyWhenQuestionHasNoChoices() {
        val essay = mcq().copy(type = QuestionType.ESSAY, choices = emptyList())
        val rows = essay.distributionOf(
            raw = mapOf("스프링" to 2),
            answer = null,
            myChoiceIndex = null
        )

        assertTrue(rows.isEmpty())
    }
}
