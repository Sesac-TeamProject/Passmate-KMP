package org.sesacteamproject.passmate.core.network.event

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// STOMP 이벤트 — 백엔드 실제 발행 형태(2026-09-03 `SessionEvent<T>`) 기준.
// 봉투는 {type, roomId, occurredAt, payload}이고, occurredAt이 재접속 시
// "스냅샷 이전 이벤트 폐기" 판정에 쓰인다 (규칙 §2-1-2).
class ServerEventEnvelopeTest {

    @Test
    fun readsOccurredAtAsFrameTimestamp() {
        val text = """
            {"type":"SCREEN_LOCKED","roomId":2,"occurredAt":"2026-09-03T12:34:56.789","payload":{"locked":true}}
        """.trimIndent()

        val frame = ServerEventDecoder.decode(text)

        assertEquals("2026-09-03T12:34:56.789", frame?.ts)
        assertEquals(ServerEvent.ScreenLocked(locked = true), frame?.event)
    }

    @Test
    fun sessionStartedHasNoPayload() {
        val text = """{"type":"SESSION_STARTED","roomId":2,"occurredAt":"2026-09-03T12:00:00"}"""

        val frame = ServerEventDecoder.decode(text)

        // 서버는 SESSION_STARTED에 페이로드를 싣지 않는다 — 프레임이 버려지면 Play로 못 넘어간다
        assertTrue(frame?.event is ServerEvent.SessionStarted)
    }

    @Test
    fun mapsQuestionStartedFromServerFieldNames() {
        val text = """
            {
              "type":"QUESTION_STARTED","roomId":2,"occurredAt":"2026-09-03T12:01:00",
              "payload":{
                "sessionQuestionId":91,"questionId":41,"orderNo":3,"totalCount":8,
                "type":"MCQ","content":"다음 중 옳은 것은?","choices":["가","나","다"],
                "points":100,"timeLimitSec":30,"endsAt":"2026-09-03T12:01:30"
              }
            }
        """.trimIndent()

        val event = ServerEventDecoder.decode(text)?.event as? ServerEvent.QuestionStarted

        assertEquals(41L, event?.questionId)
        // 서버 orderNo → 화면 문항 번호, content → 문제 본문
        assertEquals(3, event?.questionNo)
        assertEquals("다음 중 옳은 것은?", event?.body)
        assertEquals(listOf("가", "나", "다"), event?.choices)
        assertEquals("2026-09-03T12:01:30", event?.endsAt)
    }

    @Test
    fun mapsQuestionEndedWithFlatAnswerFields() {
        val text = """
            {
              "type":"QUESTION_ENDED","roomId":2,"occurredAt":"2026-09-03T12:01:30",
              "payload":{
                "sessionQuestionId":91,"questionId":41,"orderNo":3,
                "answer":"나","explanation":"나가 정답인 이유",
                "submitCount":10,"correctCount":7,"correctRate":70.0,
                "distribution":{"가":2,"나":7,"다":1}
              }
            }
        """.trimIndent()

        val event = ServerEventDecoder.decode(text)?.event as? ServerEvent.QuestionEnded

        assertEquals(3, event?.questionNo)
        // 서버는 answer·explanation을 평평하게 준다 (정답은 이 이벤트에서만 온다 — 규칙 §13)
        assertEquals("나", event?.answerReveal?.answer)
        assertEquals("나가 정답인 이유", event?.answerReveal?.explanation)
        assertEquals(7, event?.correctCount)
    }

    @Test
    fun rankingUpdatedPayloadIsBareArray() {
        val text = """
            {
              "type":"RANKING_UPDATED","roomId":2,"occurredAt":"2026-09-03T12:01:31",
              "payload":[
                {"rank":1,"participantId":5,"nickname":"준영","avatarId":"fox","totalScore":300},
                {"rank":2,"participantId":6,"nickname":"민지","avatarId":"owl","totalScore":250}
              ]
            }
        """.trimIndent()

        val event = ServerEventDecoder.decode(text)?.event as? ServerEvent.RankingUpdated

        assertEquals(2, event?.ranking?.size)
        assertEquals("준영", event?.ranking?.first()?.nickname)
        // avatarId는 문자열 키 — 화면 인덱스로 바꾼다
        assertEquals(6, event?.ranking?.first()?.avatarId)
        assertEquals(300.0, event?.ranking?.first()?.total)
    }

    @Test
    fun sessionEndedCarriesFinalRankingArray() {
        val text = """
            {
              "type":"SESSION_ENDED","roomId":2,"occurredAt":"2026-09-03T12:10:00",
              "payload":[{"rank":1,"participantId":5,"nickname":"준영","avatarId":"cat","totalScore":780}]
            }
        """.trimIndent()

        val event = ServerEventDecoder.decode(text)?.event as? ServerEvent.SessionEnded

        assertEquals(1, event?.finalRanking?.size)
        assertEquals(1, event?.finalRanking?.first()?.avatarId)
    }

    @Test
    fun unknownTypeIsDiscardedWithoutBreakingStream() {
        assertNull(ServerEventDecoder.decode("""{"type":"NOPE","roomId":2,"occurredAt":"2026-09-03T12:00:00"}"""))
        assertNull(ServerEventDecoder.decode("깨진 텍스트"))
    }
}
