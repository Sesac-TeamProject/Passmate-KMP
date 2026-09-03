package org.sesacteamproject.passmate.room.data.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import org.sesacteamproject.passmate.room.data.dto.JoinRoomResponse
import org.sesacteamproject.passmate.room.data.dto.ParticipantDto
import org.sesacteamproject.passmate.room.domain.model.StudentAvatarKeys

// 참가자 API — 백엔드 실제 응답(2026-09-03 로컬 확인) 기준.
// 서버 avatarId는 숫자가 아니라 문자열 키다(시안 "학생 아바타 — 키 이름", ERD avatar_id varchar(30)).
// 화면은 1..12 인덱스로 캐릭터를 그리므로 이 경계에서 키↔인덱스를 바꾼다.
class ParticipantMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun avatarKeyOrderMatchesPickerGrid() {
        // 시안이 정한 순서 — 바뀌면 기존 사용자 캐릭터가 전부 어긋난다
        assertEquals(
            listOf("cat", "dog", "bear", "panda", "rabbit", "fox", "frog", "penguin", "owl", "tiger", "raccoon", "dino"),
            StudentAvatarKeys.ordered
        )
        assertEquals(1, StudentAvatarKeys.toIndex("cat"))
        assertEquals(12, StudentAvatarKeys.toIndex("dino"))
        assertEquals("cat", StudentAvatarKeys.toKey(1))
        // 서버가 모르는 값을 주면 화면은 기본(cat)으로 접는다
        assertNull(StudentAvatarKeys.toIndex("unknown-avatar"))
        assertNull(StudentAvatarKeys.toIndex(null))
    }

    @Test
    fun parsesBareParticipantArray() {
        val raw = """
            [
              {"id": 5, "nickname": "준영", "avatarId": "fox", "isGuest": true, "joinedAt": "2026-09-03T11:57:48"},
              {"id": 6, "nickname": "민지", "avatarId": "owl", "isGuest": false, "joinedAt": "2026-09-03T11:58:10"}
            ]
        """.trimIndent()

        val participants = json.decodeFromString<List<ParticipantDto>>(raw).map { it.toDomain() }

        assertEquals(2, participants.size)
        assertEquals(5L, participants.first().participantId)
        assertEquals("준영", participants.first().nickname)
        // fox는 6번째 키
        assertEquals(6, participants.first().avatarId)
        assertEquals(true, participants.first().isGuest)
        assertEquals(9, participants.last().avatarId)
        assertEquals(false, participants.last().isGuest)
    }

    @Test
    fun parsesJoinResponseWithNestedParticipantAndToken() {
        val raw = """
            {
              "participant": {"id": 5, "nickname": "준영", "avatarId": "cat", "isGuest": true, "joinedAt": "2026-09-03T11:57:48"},
              "accessToken": "guest-jwt-token"
            }
        """.trimIndent()

        val response = json.decodeFromString<JoinRoomResponse>(raw)

        assertEquals(5L, response.participant.id)
        assertEquals("guest-jwt-token", response.accessToken)
        assertEquals(1, response.participant.toDomain().avatarId)
    }
}
