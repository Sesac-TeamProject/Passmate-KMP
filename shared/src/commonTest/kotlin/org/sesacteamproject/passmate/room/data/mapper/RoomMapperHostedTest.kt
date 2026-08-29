package org.sesacteamproject.passmate.room.data.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import org.sesacteamproject.passmate.room.data.dto.HostedRoomsResponse
import org.sesacteamproject.passmate.room.domain.model.RoomStatus

class RoomMapperHostedTest {

    @Test
    fun mapsHostedRoomsAndSplitsByStatus() {
        val response = HostedRoomsResponse(
            items = listOf(
                HostedRoomsResponse.HostedRoomDto(
                    roomId = 1,
                    pin = "482913",
                    title = "Spring 실전 모의고사 4주차",
                    status = "RUNNING",
                    participantCount = 24,
                    scheduledAt = "2026-08-29T20:00:00+09:00"
                ),
                HostedRoomsResponse.HostedRoomDto(
                    roomId = 2,
                    pin = "111222",
                    title = "네트워크 한 번에 정리",
                    status = "ENDED",
                    endedAtLabel = "8/19",
                    participantCount = 9,
                    avgAccuracyPercent = 77
                )
            ),
            nextCursor = "c2",
            hasNext = true
        )

        val page = response.toDomain()

        assertEquals(2, page.items.size)
        assertEquals(RoomStatus.RUNNING, page.items.first().status)
        assertEquals(true, page.items.first().isOngoing)
        // 서버 wire 값 ENDED는 FINISHED로 매핑되고, 진행 중 판정에서 제외된다
        assertEquals(RoomStatus.FINISHED, page.items.last().status)
        assertEquals(false, page.items.last().isOngoing)
        assertEquals(77, page.items.last().avgAccuracyPercent)
        assertEquals("c2", page.nextCursor)
    }
}
