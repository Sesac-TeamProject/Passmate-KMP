package org.sesacteamproject.passmate.user.data.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import org.sesacteamproject.passmate.user.data.dto.MyPageResponse

// GET /users/me/rooms/joined — 백엔드 실제 응답(2026-09-03 로컬 확인) 기준.
// summary + rooms(page 응답) 중첩 구조이고, 정답률은 서버가 0~100 퍼센트로 준다
// (백엔드 `ParticipantReport.accuracyOf` = correctCount * 100.0 / totalQuestions).
class MyPageMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun mapsSummaryAndPagedRooms() {
        val raw = """
            {
              "summary": {
                "completedSessionCount": 4,
                "averageAccuracy": 71.43,
                "averageRank": 3.25,
                "weakTopics": ["JPA 영속성", "트랜잭션"]
              },
              "rooms": {
                "content": [
                  {
                    "roomId": 401,
                    "title": "7월 3주차 미적분 특강",
                    "hostNickname": "김선생",
                    "status": "ENDED",
                    "startedAt": "2026-07-18T20:00:00",
                    "endedAt": "2026-07-18T21:10:00",
                    "questionCount": 10,
                    "myScore": 890,
                    "myRank": 2,
                    "myAccuracy": 80.0,
                    "hasReport": true
                  }
                ],
                "page": 0,
                "size": 20,
                "totalElements": 4,
                "totalPages": 2,
                "hasNext": true
              }
            }
        """.trimIndent()

        val myPage = json.decodeFromString<MyPageResponse>(raw).toDomain()
        val room = myPage.rooms.first()

        assertEquals(4, myPage.summary.participationCount)
        // 서버가 이미 퍼센트(0~100)로 준다 — 반올림만 한다
        assertEquals(71, myPage.summary.accuracyPercent)
        assertEquals(3.25, myPage.summary.avgRank)
        assertEquals(listOf("JPA 영속성", "트랜잭션"), myPage.summary.weakTopics)
        // 서버 JoinedSummary에 추이 문구가 없다 — 계약 갱신 전까지 null
        assertNull(myPage.summary.trendText)
        // 서버가 진행 중 방을 주지 않는다
        assertNull(myPage.ongoing)

        assertEquals(1, myPage.rooms.size)
        assertEquals(401L, room.roomId)
        // 종료 시각의 날짜 부분을 화면 표기(YYYY.MM.DD)로 바꾼다
        assertEquals("2026.07.18", room.dateLabel)
        assertEquals(10, room.questionCount)
        assertEquals(890.0, room.myScore)
        assertEquals(2, room.myRank)
        assertEquals(true, room.hasReport)

        // page 기반 — 다음 페이지 번호를 커서 자리에 싣는다
        assertEquals("1", myPage.nextCursor)
    }

    @Test
    fun mapsEmptyStateWithoutFailing() {
        val raw = """
            {
              "summary": {"completedSessionCount":0,"averageAccuracy":0.0,"averageRank":0.0,"weakTopics":[]},
              "rooms": {"content":[],"page":0,"size":20,"totalElements":0,"totalPages":0,"hasNext":false}
            }
        """.trimIndent()

        val myPage = json.decodeFromString<MyPageResponse>(raw).toDomain()

        assertEquals(0, myPage.summary.participationCount)
        assertEquals(0, myPage.rooms.size)
        assertNull(myPage.nextCursor)
    }
}
