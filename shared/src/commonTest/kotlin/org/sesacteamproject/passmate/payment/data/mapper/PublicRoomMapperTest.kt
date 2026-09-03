package org.sesacteamproject.passmate.payment.data.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import org.sesacteamproject.passmate.payment.data.dto.PublicRoomPageResponse
import org.sesacteamproject.passmate.room.domain.model.RoomStatus

// GET /rooms/public — 백엔드 실제 응답(2026-09-03 로컬 확인)을 그대로 역직렬화해 매핑을 검증한다.
// 서버는 non_null 직렬화라 값이 없는 필드는 키 자체가 생략된다.
class PublicRoomMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parsesServerPageShapeAndMapsRooms() {
        val raw = """
            {
              "content": [
                {
                  "id": 1,
                  "title": "계약 정합 검증방",
                  "status": "WAITING",
                  "type": "FREE",
                  "questionCount": 2,
                  "participantCount": 1,
                  "host": { "userId": 3, "nickname": "개발계정 host1" }
                },
                {
                  "id": 2,
                  "title": "DTO 정합 확인용 방",
                  "topic": "네트워크",
                  "status": "WAITING",
                  "type": "PAID",
                  "fee": 500,
                  "participantCount": 0,
                  "maxParticipants": 30,
                  "host": { "userId": 2, "nickname": "연결확인" }
                }
              ],
              "page": 0,
              "size": 20,
              "totalElements": 2,
              "totalPages": 1,
              "hasNext": false
            }
        """.trimIndent()

        val page = json.decodeFromString<PublicRoomPageResponse>(raw).toDomain()
        val first = page.items.first()
        val second = page.items.last()

        assertEquals(2, page.items.size)
        assertEquals(false, page.hasNext)

        // 서버 id → 도메인 roomId
        assertEquals(1L, first.roomId)
        assertEquals("계약 정합 검증방", first.title)
        assertEquals(RoomStatus.WAITING, first.status)
        // host는 중첩 객체 — userId·nickname만 준다
        assertEquals(3L, first.hostId)
        assertEquals("개발계정 host1", first.hostName)
        // 서버가 등급·별점을 주지 않으므로 null로 내려간다
        assertNull(first.hostLevel)
        assertNull(first.hostRating)
        // type=FREE → 무료, fee 키 자체가 생략된다
        assertEquals(false, first.isPaid)
        assertNull(first.entryFee)

        // type=PAID → 유료, fee → entryFee
        assertEquals(2L, second.roomId)
        assertEquals("네트워크", second.topic)
        assertEquals(true, second.isPaid)
        assertEquals(500, second.entryFee)
        assertEquals(30, second.maxParticipants)
    }

    @Test
    fun mapsEmptyPageWithoutFailing() {
        val raw = """
            {"content":[],"page":0,"size":20,"totalElements":0,"totalPages":0,"hasNext":false}
        """.trimIndent()

        val page = json.decodeFromString<PublicRoomPageResponse>(raw).toDomain()

        assertEquals(0, page.items.size)
        assertEquals(false, page.hasNext)
    }
}
