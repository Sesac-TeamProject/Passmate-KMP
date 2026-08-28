package org.sesacteamproject.passmate.user.data.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.sesacteamproject.passmate.user.data.dto.MyPageResponse

class UserMapperTest {

    @Test
    fun mapsSummaryOngoingAndRooms() {
        val response = MyPageResponse(
            summary = MyPageResponse.SummaryDto(
                participationCount = 3,
                accuracyPercent = 71,
                avgRank = 3.3,
                trendText = "지난주보다 정답률이 8%p 올랐어요",
                weakTopics = listOf("JPA 영속성", "트랜잭션")
            ),
            ongoing = MyPageResponse.OngoingDto(
                roomId = 9,
                pin = "482913",
                title = "Spring 실전 모의고사 4주차",
                hostNickname = "김선생",
                progressLabel = "3 / 8 문항 진행 중"
            ),
            rooms = listOf(
                MyPageResponse.RoomDto(
                    roomId = 1,
                    title = "Spring 스터디",
                    dateLabel = "8/22 (금)",
                    questionCount = 8,
                    myScore = 990.0,
                    myRank = 3,
                    hasReport = true
                )
            ),
            nextCursor = "c2"
        )

        val myPage = response.toDomain()

        assertEquals(3, myPage.summary.participationCount)
        assertEquals(71, myPage.summary.accuracyPercent)
        assertEquals(listOf("JPA 영속성", "트랜잭션"), myPage.summary.weakTopics)
        assertEquals(9L, myPage.ongoing?.roomId)
        assertEquals("482913", myPage.ongoing?.pin)
        assertEquals(1, myPage.rooms.size)
        assertEquals(990.0, myPage.rooms.first().myScore)
        assertEquals(true, myPage.rooms.first().hasReport)
        assertEquals("c2", myPage.nextCursor)
    }

    @Test
    fun mapsWithoutOngoingAndCursor() {
        val response = MyPageResponse(
            summary = MyPageResponse.SummaryDto(participationCount = 1, accuracyPercent = 50),
            rooms = emptyList()
        )

        val myPage = response.toDomain()

        assertNull(myPage.ongoing)
        assertNull(myPage.nextCursor)
        assertEquals(0, myPage.rooms.size)
    }
}
