package org.sesacteamproject.passmate.room.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals

// 게스트 토큰은 방 하나에 묶인다 — 다른 방 토큰으로 재입장을 시도하면 남의 방에서 내 기록을 찾는 꼴이다
class GuestSessionMatchTest {

    @Test
    fun matchesOnlyWhenTokenBelongsToThisRoom() {
        assertEquals(true, hasGuestSessionFor(roomId = 1L, guestToken = "t", guestRoomId = 1L))
        assertEquals(false, hasGuestSessionFor(roomId = 2L, guestToken = "t", guestRoomId = 1L))
    }

    @Test
    fun noTokenMeansNoSession() {
        assertEquals(false, hasGuestSessionFor(roomId = 1L, guestToken = null, guestRoomId = 1L))
        assertEquals(false, hasGuestSessionFor(roomId = 1L, guestToken = "t", guestRoomId = null))
    }
}
