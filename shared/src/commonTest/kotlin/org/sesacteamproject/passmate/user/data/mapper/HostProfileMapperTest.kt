package org.sesacteamproject.passmate.user.data.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.sesacteamproject.passmate.room.domain.model.HostLevel
import org.sesacteamproject.passmate.user.data.dto.HostProfileResponse
import org.sesacteamproject.passmate.user.domain.model.BadgeType

// GET /users/{userId}/profile — 백엔드 실제 응답(2026-09-07 로컬 확인) 기준.
// 계약 문서는 이 엔드포인트를 "목만"으로 적어 두었으나 실제로는 동작한다.
// 앱이 badges를 문자열 배열로 받던 동안 M-10 선생님 프로필 시트는 파싱 단계에서 실패했다.
class HostProfileMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun mapsServerFieldNamesAndObjectBadges() {
        val raw = """
            {
              "userId": 2,
              "nickname": "김민지",
              "defaultAvatarId": "fox",
              "activeSince": "2026-09-03T02:00:01.864375",
              "level": 3,
              "levelName": "검증된 운영자",
              "avgRating": 4.6,
              "ratingCount": 32,
              "roomsHosted": 24,
              "totalStudents": 312,
              "badgeCount": 2,
              "badges": [
                {"code":"FIRST_ROOM","name":"첫 방 개설","achieved":true,"achievedAt":"2026-09-03T03:40:20"},
                {"code":"ROOMS_10","name":"방 10회 운영","achieved":false,"progress":9,"target":10.0}
              ],
              "openRooms": [
                {"id":22,"title":"OX 제출 검증2","status":"RUNNING","type":"FREE","questionCount":2,"participantCount":1}
              ]
            }
        """.trimIndent()

        val profile = json.decodeFromString<HostProfileResponse>(raw).toDomain()

        assertEquals("김민지", profile.nickname)
        assertEquals(HostLevel.VERIFIED, profile.level)
        // 서버는 avgRating·roomsHosted로 준다 (앱은 avgStars·roomCount로 쓴다)
        assertEquals(4.6, profile.avgStars)
        assertEquals(24, profile.roomCount)
        assertEquals(312, profile.totalStudents)
        // 문자열 키 → 화면 인덱스 (fox는 시안 그리드 6번째)
        assertEquals(6, profile.avatarId)
        // 획득한 뱃지만 보여준다
        assertEquals(listOf(BadgeType.FIRST_ROOM), profile.badges)
        assertEquals(1, profile.rooms.size)
    }

    @Test
    fun toleratesMinimalResponse() {
        val profile = json.decodeFromString<HostProfileResponse>("""{"userId":9,"nickname":"준영"}""").toDomain()

        assertNull(profile.level)
        assertNull(profile.avatarId)
        assertTrue(profile.badges.isEmpty())
        assertTrue(profile.rooms.isEmpty())
    }
}
