package org.sesacteamproject.passmate.user.data.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import org.sesacteamproject.passmate.user.data.dto.BadgesResponse
import org.sesacteamproject.passmate.user.data.dto.GradeResponse
import org.sesacteamproject.passmate.user.domain.model.BadgeType
import org.sesacteamproject.passmate.room.domain.model.HostLevel

// 명성·뱃지 — 백엔드 실제 응답(2026-09-03 로컬 확인) 기준.
class MyPageDetailMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun mapsGradeWithFlatServerFields() {
        val raw = """
            {
              "level": 3,
              "levelName": "인증 선생님",
              "levelAchievedAt": "2026-08-01T10:00:00",
              "roomsHosted": 24,
              "totalStudents": 312,
              "avgRating": 4.6,
              "ratingCount": 128,
              "nextLevel": 4,
              "nextLevelName": "우수 선생님",
              "nextRequirements": [
                {"type": "ROOMS_HOSTED", "label": "방 40회 운영", "current": 24.0, "target": 40.0, "met": false},
                {"type": "AVG_RATING", "label": "평균 별점 4.5", "current": 4.6, "target": 4.5, "met": true}
              ],
              "nextLevelProgress": 0.8,
              "ratingSamplePending": false,
              "unlocked": [],
              "lastEvaluatedAt": "2026-09-01T00:00:00"
            }
        """.trimIndent()

        val grade = json.decodeFromString<GradeResponse>(raw).toDomain()

        assertEquals(HostLevel.from(3), grade.level)
        assertEquals("2026-08-01T10:00:00", grade.achievedAt)
        assertEquals(24, grade.stats.roomCount)
        assertEquals(312, grade.stats.totalStudents)
        assertEquals(4.6, grade.stats.avgStars)
        assertEquals(128, grade.stats.ratingCount)

        // nextLevelProgress는 0.0~1.0 비율 — 화면은 퍼센트로 그린다
        assertEquals(80, grade.next?.progressPercent)
        assertEquals(HostLevel.from(4), grade.next?.level)
        assertEquals(2, grade.next?.criteria?.size)
        assertEquals("방 40회 운영", grade.next?.criteria?.first()?.label)
        assertEquals(false, grade.next?.criteria?.first()?.met)
    }

    @Test
    fun maxLevelHasNoNextGrade() {
        val raw = """{"level":5,"levelName":"마스터","roomsHosted":80,"totalStudents":900,"ratingCount":300}"""

        val grade = json.decodeFromString<GradeResponse>(raw).toDomain()

        assertNull(grade.next)
    }

    @Test
    fun mapsBadgeCollection() {
        val raw = """
            {
              "achievedCount": 2,
              "totalCount": 8,
              "badges": [
                {"code":"FIRST_ROOM","name":"첫 방 개설","achieved":true,"achievedAt":"2026-07-01T09:00:00"},
                {"code":"ROOMS_10","name":"방 10회 운영","achieved":false,"progress":4,"target":10},
                {"code":"UNKNOWN_BADGE","name":"모르는 뱃지","achieved":false}
              ]
            }
        """.trimIndent()

        val badges = json.decodeFromString<BadgesResponse>(raw).toDomain()

        // 서버가 모르는 코드를 주면 화면에서 접는다
        assertEquals(2, badges.size)
        assertEquals(BadgeType.FIRST_ROOM, badges.first().type)
        assertEquals(true, badges.first().earned)
        assertEquals(4, badges.last().progressCurrent)
        assertEquals(10, badges.last().progressTarget)
    }
}
