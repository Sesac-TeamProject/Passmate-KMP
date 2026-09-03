package org.sesacteamproject.passmate.core.network.event

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

// 봉투와 세션 이벤트 매핑은 ServerEventEnvelopeTest가 덮는다.
// 여기서는 나머지 이벤트와 손상 프레임 처리를 본다.
class ServerEventDecoderTest {

    @Test
    fun decodeParticipantJoined() {
        val text = """
            {"type":"PARTICIPANT_JOINED","roomId":2,"occurredAt":"2026-08-27T10:00:00",
             "payload":{"participantId":11,"nickname":"영희","isGuest":true,"avatarId":"fox","count":3}}
        """.trimIndent()

        val frame = ServerEventDecoder.decode(text)
        val event = assertIs<ServerEvent.ParticipantJoined>(frame?.event)

        assertEquals("2026-08-27T10:00:00", frame?.ts)
        assertEquals(11L, event.participantId)
        assertEquals("영희", event.nickname)
        assertEquals(true, event.isGuest)
        assertEquals(3, event.count)
        // avatarId는 문자열 키 — 화면 인덱스로 바뀐다 (fox는 6번째)
        assertEquals(6, event.avatarId)
    }

    @Test
    fun decodeParticipantLeftWithReason() {
        val text = """
            {"type":"PARTICIPANT_LEFT","roomId":2,"occurredAt":"2026-08-27T10:00:30",
             "payload":{"participantId":11,"count":2,"reason":"KICKED"}}
        """.trimIndent()

        val frame = ServerEventDecoder.decode(text)
        val event = assertIs<ServerEvent.ParticipantLeft>(frame?.event)

        assertEquals(11L, event.participantId)
        assertEquals(ServerEvent.ParticipantLeft.REASON_KICKED, event.reason)
    }

    @Test
    fun decodeHintPublished() {
        val text = """
            {"type":"HINT_PUBLISHED","roomId":2,"occurredAt":"2026-08-27T10:04:00",
             "payload":{"hintId":7,"questionNo":2,"clipUrl":"https://storage/clip.webm","durationMs":4200}}
        """.trimIndent()

        val frame = ServerEventDecoder.decode(text)
        val event = assertIs<ServerEvent.HintPublished>(frame?.event)

        assertEquals(4200L, event.durationMs)
    }

    @Test
    fun malformedFrameReturnsNullWithoutBreakingStream() {
        assertNull(ServerEventDecoder.decode("not-json"))
        // type 없음
        assertNull(ServerEventDecoder.decode("""{"occurredAt":"2026-08-27T10:06:00","payload":{}}"""))
        // occurredAt 없음 — 스냅샷 비교 기준이 없으면 프레임을 쓸 수 없다 (규칙 §2-1-2)
        assertNull(ServerEventDecoder.decode("""{"type":"SCREEN_LOCKED","payload":{"locked":true}}"""))
        // payload 구조가 다르면(객체 자리에 문자열) 해당 프레임만 버린다.
        // 필드 누락은 기본값으로 관대하게 받는다 — 서버가 필드를 늘려도 스트림이 끊기지 않는다.
        assertNull(
            ServerEventDecoder.decode(
                """{"type":"PARTICIPANT_JOINED","occurredAt":"2026-08-27T10:07:00","payload":"문자열"}"""
            )
        )
    }
}
