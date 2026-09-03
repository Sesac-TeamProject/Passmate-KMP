package org.sesacteamproject.passmate.room.data.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import org.sesacteamproject.passmate.room.data.dto.HostedRoomsResponse
import org.sesacteamproject.passmate.room.domain.model.RoomStatus

// GET /users/me/rooms/hosted — 백엔드 실제 응답(2026-09-03 로컬 확인) 기준.
// 서버가 진행 중(active)·종료(ended)를 나눠 주고 형태도 서로 다르다. 페이징은 없다.
class HostedRoomsMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun mapsActiveAndEndedRoomsIntoOneList() {
        val raw = """
            {
              "reputation": {
                "level": 3,
                "nextLevelProgress": 0.4,
                "hostedSessionCount": 24,
                "totalStudentCount": 312,
                "averageStars": 4.6,
                "ratingCount": 128
              },
              "active": [
                {
                  "roomId": 301,
                  "title": "8월 4주차 Spring 스터디",
                  "pin": "482913",
                  "status": "RUNNING",
                  "scheduledAt": "2026-08-29T20:00:00",
                  "participantCount": 12,
                  "currentQuestionNo": 3
                }
              ],
              "ended": [
                {
                  "roomId": 302,
                  "title": "네트워크 한 번에 정리",
                  "endedAt": "2026-08-19T21:30:00",
                  "studentCount": 9,
                  "correctRate": 77.5,
                  "averageStars": 4.2,
                  "ratingCount": 6
                }
              ]
            }
        """.trimIndent()

        val page = json.decodeFromString<HostedRoomsResponse>(raw).toDomain()
        val active = page.items.first()
        val ended = page.items.last()

        assertEquals(2, page.items.size)

        assertEquals(301L, active.roomId)
        assertEquals("482913", active.pin)
        assertEquals(RoomStatus.RUNNING, active.status)
        assertEquals(true, active.isOngoing)
        assertEquals(12, active.participantCount)

        // 종료 방에는 pin·status가 없다 — 종료로 간주하고 화면도 PIN을 쓰지 않는다
        assertEquals(302L, ended.roomId)
        assertEquals(RoomStatus.FINISHED, ended.status)
        assertEquals(false, ended.isOngoing)
        assertEquals("2026.08.19", ended.endedAtLabel)
        assertEquals(9, ended.participantCount)
        // correctRate는 서버가 0~100 퍼센트로 준다 (SessionService: correctCount * 100.0 / submitCount)
        assertEquals(78, ended.avgAccuracyPercent)

        // 서버가 페이징하지 않는다
        assertNull(page.nextCursor)
        assertEquals(false, page.hasNext)
    }

    @Test
    fun mapsEmptyStateWithoutFailing() {
        val raw = """{"reputation":{"level":1},"active":[],"ended":[]}"""

        val page = json.decodeFromString<HostedRoomsResponse>(raw).toDomain()

        assertEquals(0, page.items.size)
        assertNull(page.nextCursor)
    }
}
