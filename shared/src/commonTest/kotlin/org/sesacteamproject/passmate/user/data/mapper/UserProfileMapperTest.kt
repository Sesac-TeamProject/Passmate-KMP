package org.sesacteamproject.passmate.user.data.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import org.sesacteamproject.passmate.user.data.dto.UserProfileResponse

// GET /users/me — 백엔드 실제 응답(2026-09-07 로컬 확인) 기준.
// 서버는 카운트를 stats{}에 중첩해 주고, 코인은 coinBalance, 캐릭터는 defaultAvatarId(문자열 키)다.
// 앱이 최상위 joinedRoomCount·coins·avatarId를 읽던 동안 마이 화면 숫자가 전부 0으로 나왔다.
class UserProfileMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun mapsNestedStatsAndCoinBalance() {
        val raw = """
            {
              "id": 2,
              "nickname": "연결확인",
              "email": "kmp-integration-check@dev.passmate.local",
              "provider": "GOOGLE",
              "isAdmin": false,
              "joinedAt": "2026-09-03T02:00:01.864375",
              "lastLoginAt": "2026-09-06T18:15:24.510408",
              "defaultAvatarId": "fox",
              "stats": {
                "joinedRoomCount": 1,
                "hostedRoomCount": 17,
                "hostedSessionCount": 9,
                "totalStudentCount": 17
              },
              "coinBalance": 1200
            }
        """.trimIndent()

        val profile = json.decodeFromString<UserProfileResponse>(raw).toDomain()

        assertEquals("연결확인", profile.nickname)
        assertEquals(1, profile.joinedRoomCount)
        assertEquals(17, profile.hostedRoomCount)
        assertEquals(1200L, profile.coins)
        // 문자열 키 → 화면 인덱스 (fox는 시안 그리드 6번째)
        assertEquals(6, profile.avatarId)
    }

    // 캐릭터를 한 번도 안 고른 계정은 defaultAvatarId·stats가 통째로 빠진다
    @Test
    fun toleratesMissingAvatarAndStats() {
        val raw = """{"id":2,"nickname":"준영","provider":"GOOGLE","coinBalance":0}"""

        val profile = json.decodeFromString<UserProfileResponse>(raw).toDomain()

        assertNull(profile.avatarId)
        assertNull(profile.joinedRoomCount)
        assertEquals(0L, profile.coins)
    }

    // 서버가 모르는 키를 주면 화면은 기본 캐릭터로 접는다 (시안 아바타 시트)
    @Test
    fun foldsUnknownAvatarKeyToNull() {
        val raw = """{"nickname":"준영","defaultAvatarId":"unicorn"}"""

        assertNull(json.decodeFromString<UserProfileResponse>(raw).toDomain().avatarId)
    }
}
