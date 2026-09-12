package org.sesacteamproject.passmate.user.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// 서버 PUT /users/me는 전체 교체다 — 빠진 필드는 보존되지 않고 지워진다.
// 그래서 이번에 바꾸지 않는 값도 현재 프로필에서 채워 보내야 한다 (2026-09-09 실측 회귀).
class ProfileUpdateResolverTest {

    @Test
    fun 닉네임만_바꿔도_현재_캐릭터를_실어_보낸다() {
        val request = resolveProfileUpdate(
            requestedNickname = "새닉네임",
            requestedAvatarKey = null,
            currentNickname = "옛닉네임",
            currentAvatarKey = "bear"
        )

        assertEquals("새닉네임", request?.nickname)
        assertEquals("bear", request?.defaultAvatarId)
    }

    @Test
    fun 캐릭터만_바꿔도_현재_닉네임을_실어_보낸다() {
        val request = resolveProfileUpdate(
            requestedNickname = null,
            requestedAvatarKey = "fox",
            currentNickname = "서승혁",
            currentAvatarKey = "bear"
        )

        assertEquals("서승혁", request?.nickname)
        assertEquals("fox", request?.defaultAvatarId)
    }

    @Test
    fun 둘_다_주어지면_그대로_보낸다() {
        val request = resolveProfileUpdate(
            requestedNickname = "새닉네임",
            requestedAvatarKey = "dino",
            currentNickname = null,
            currentAvatarKey = null
        )

        assertEquals("새닉네임", request?.nickname)
        assertEquals("dino", request?.defaultAvatarId)
    }

    @Test
    fun 캐릭터를_한_번도_고른_적_없으면_null_그대로_둔다() {
        val request = resolveProfileUpdate(
            requestedNickname = "새닉네임",
            requestedAvatarKey = null,
            currentNickname = "옛닉네임",
            currentAvatarKey = null
        )

        assertEquals("새닉네임", request?.nickname)
        assertNull(request?.defaultAvatarId)
    }

    @Test
    fun 공백_닉네임은_현재_닉네임으로_대체한다() {
        val request = resolveProfileUpdate(
            requestedNickname = "   ",
            requestedAvatarKey = "cat",
            currentNickname = "서승혁",
            currentAvatarKey = null
        )

        assertEquals("서승혁", request?.nickname)
        assertEquals("cat", request?.defaultAvatarId)
    }

    @Test
    fun 닉네임을_끝내_못_구하면_null을_반환한다() {
        val request = resolveProfileUpdate(
            requestedNickname = null,
            requestedAvatarKey = "cat",
            currentNickname = null,
            currentAvatarKey = null
        )

        assertNull(request)
    }
}
