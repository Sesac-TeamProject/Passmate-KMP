package org.sesacteamproject.passmate.room.data.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import org.sesacteamproject.passmate.room.data.dto.RoomInfoResponse
import org.sesacteamproject.passmate.room.domain.model.RoomStatus

// GET /rooms/pin/{pin} — 백엔드 실제 응답(2026-09-03 로컬 확인)을 그대로 역직렬화한다.
// 응답에는 pin이 없다(계약 `RoomSummaryResponse`) — 조회 키로 쓴 pin을 그대로 채운다.
class RoomInfoMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parsesServerSummaryAndKeepsRequestedPin() {
        val raw = """
            {
              "id": 2,
              "title": "DTO 정합 확인용 방",
              "topic": "네트워크",
              "status": "WAITING",
              "type": "FREE",
              "participantCount": 0,
              "maxParticipants": 30,
              "guestAllowed": true
            }
        """.trimIndent()

        val room = json.decodeFromString<RoomInfoResponse>(raw).toDomain(pin = "370369")

        assertEquals(2L, room.roomId)
        // 응답에 없는 pin은 요청에 쓴 값을 그대로 유지한다
        assertEquals("370369", room.pin)
        assertEquals("DTO 정합 확인용 방", room.title)
        assertEquals("네트워크", room.topic)
        assertEquals(RoomStatus.WAITING, room.status)
        assertEquals(0, room.participantCount)
        assertEquals(30, room.maxParticipants)
        assertEquals(false, room.isPaid)
        assertNull(room.entryFee)
        // 서버 `RoomSummaryResponse`에 없는 값들 — 계약 갱신 전까지 null
        assertNull(room.questionCount)
        assertNull(room.estimatedMinutes)
        assertNull(room.host)
    }

    @Test
    fun mapsPaidRoomFeeAndType() {
        val raw = """
            {
              "id": 9,
              "title": "유료 모의고사",
              "status": "RUNNING",
              "type": "PAID",
              "fee": 500,
              "participantCount": 12,
              "guestAllowed": false
            }
        """.trimIndent()

        val room = json.decodeFromString<RoomInfoResponse>(raw).toDomain(pin = "111222")

        assertEquals(true, room.isPaid)
        assertEquals(500, room.entryFee)
        assertEquals(RoomStatus.RUNNING, room.status)
    }
}
