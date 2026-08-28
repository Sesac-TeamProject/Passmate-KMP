package org.sesacteamproject.passmate.core.network.event

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ServerEventDecoderTest {

    @Test
    fun decodeParticipantJoined() {
        val text = """
            {"type":"PARTICIPANT_JOINED","ts":"2026-08-27T10:00:00Z",
             "data":{"participantId":11,"nickname":"영희","isGuest":true,"avatarId":5,"count":3}}
        """.trimIndent()

        val frame = ServerEventDecoder.decode(text)
        val event = assertIs<ServerEvent.ParticipantJoined>(frame?.event)

        assertEquals("2026-08-27T10:00:00Z", frame?.ts)
        assertEquals(11L, event.participantId)
        assertEquals("영희", event.nickname)
        assertEquals(true, event.isGuest)
        assertEquals(5, event.avatarId)
        assertEquals(3, event.count)
    }

    @Test
    fun decodeParticipantLeftWithReason() {
        val text = """
            {"type":"PARTICIPANT_LEFT","ts":"2026-08-27T10:00:30Z",
             "data":{"participantId":11,"count":2,"reason":"KICKED"}}
        """.trimIndent()

        val frame = ServerEventDecoder.decode(text)
        val event = assertIs<ServerEvent.ParticipantLeft>(frame?.event)

        assertEquals(11L, event.participantId)
        assertEquals(ServerEvent.ParticipantLeft.REASON_KICKED, event.reason)
    }

    @Test
    fun decodeSessionStarted() {
        val text = """
            {"type":"SESSION_STARTED","ts":"2026-08-27T10:00:40Z",
             "data":{"sessionId":3,"questionCount":10}}
        """.trimIndent()

        val frame = ServerEventDecoder.decode(text)
        val event = assertIs<ServerEvent.SessionStarted>(frame?.event)

        assertEquals(3L, event.sessionId)
        assertEquals(10, event.questionCount)
    }

    @Test
    fun decodeQuestionStartedWithoutAnswerField() {
        val text = """
            {"type":"QUESTION_STARTED","ts":"2026-08-27T10:01:00Z",
             "data":{"questionNo":2,"type":"MULTIPLE_CHOICE","body":"1+1은?",
                     "choices":["1","2","3","4"],"points":10,"timeLimitSec":30,
                     "endsAt":"2026-08-27T10:01:30Z"}}
        """.trimIndent()

        val frame = ServerEventDecoder.decode(text)
        val event = assertIs<ServerEvent.QuestionStarted>(frame?.event)

        assertEquals(2, event.questionNo)
        assertEquals(listOf("1", "2", "3", "4"), event.choices)
        assertEquals("2026-08-27T10:01:30Z", event.endsAt)
    }

    @Test
    fun decodeRankingUpdated() {
        val text = """
            {"type":"RANKING_UPDATED","ts":"2026-08-27T10:02:00Z",
             "data":{"ranking":[{"rank":1,"participantId":11,"nickname":"영희","total":150.0}]}}
        """.trimIndent()

        val frame = ServerEventDecoder.decode(text)
        val event = assertIs<ServerEvent.RankingUpdated>(frame?.event)

        assertEquals(1, event.ranking.size)
        assertEquals(150.0, event.ranking.first().total)
    }

    @Test
    fun decodeQuestionEndedWithReveal() {
        val text = """
            {"type":"QUESTION_ENDED","ts":"2026-08-27T10:03:00Z",
             "data":{"questionNo":2,"answerReveal":{"answer":"2","explanation":"1+1=2"},"correctCount":15}}
        """.trimIndent()

        val frame = ServerEventDecoder.decode(text)
        val event = assertIs<ServerEvent.QuestionEnded>(frame?.event)

        assertEquals("2", event.answerReveal.answer)
        assertEquals(15, event.correctCount)
    }

    @Test
    fun decodeHintPublished() {
        val text = """
            {"type":"HINT_PUBLISHED","ts":"2026-08-27T10:04:00Z",
             "data":{"hintId":7,"questionNo":2,"clipUrl":"https://storage/clip.webm","durationMs":4200}}
        """.trimIndent()

        val frame = ServerEventDecoder.decode(text)
        val event = assertIs<ServerEvent.HintPublished>(frame?.event)

        assertEquals(4200L, event.durationMs)
    }

    @Test
    fun decodeScreenLocked() {
        val text = """{"type":"SCREEN_LOCKED","ts":"2026-08-27T10:04:30Z","data":{"locked":true}}"""

        val frame = ServerEventDecoder.decode(text)
        val event = assertIs<ServerEvent.ScreenLocked>(frame?.event)

        assertEquals(true, event.locked)
    }

    @Test
    fun unknownTypeReturnsNull() {
        val text = """{"type":"NOT_IN_CONTRACT","ts":"2026-08-27T10:05:00Z","data":{}}"""

        assertNull(ServerEventDecoder.decode(text))
    }

    @Test
    fun malformedPayloadReturnsNull() {
        assertNull(ServerEventDecoder.decode("not-json"))
        assertNull(ServerEventDecoder.decode("""{"ts":"2026-08-27T10:06:00Z","data":{}}"""))
        assertNull(
            ServerEventDecoder.decode(
                """{"type":"PARTICIPANT_JOINED","ts":"2026-08-27T10:07:00Z","data":{"nickname":"영희"}}"""
            )
        )
    }
}
