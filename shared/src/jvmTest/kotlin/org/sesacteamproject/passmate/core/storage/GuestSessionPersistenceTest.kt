package org.sesacteamproject.passmate.core.storage

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// 게스트 토큰이 앱을 껐다 켜도 남아야 재입장으로 원래 자리에 돌아갈 수 있다.
// 인스턴스를 새로 만드는 것이 "앱 재실행"에 해당한다 (jvm actual = java Preferences)
class GuestSessionPersistenceTest {

    private fun freshStorage(): TokenStorage {
        return TokenStorage()
    }

    @BeforeTest
    fun setUp() {
        freshStorage().clearGuestSession()
    }

    @AfterTest
    fun tearDown() {
        freshStorage().clearGuestSession()
    }

    @Test
    fun guestSessionSurvivesNewStorageInstance() {
        freshStorage().saveGuestSession(token = "guest-jwt", roomId = 42L)

        val reopened = freshStorage()

        assertEquals("guest-jwt", reopened.guestToken)
        assertEquals(42L, reopened.guestRoomId)
    }

    @Test
    fun clearingGuestSessionRemovesBothTokenAndRoom() {
        freshStorage().saveGuestSession(token = "guest-jwt", roomId = 42L)

        freshStorage().clearGuestSession()

        val reopened = freshStorage()

        assertNull(reopened.guestToken)
        assertNull(reopened.guestRoomId)
    }

    // 다른 방에 들어가면 이전 방 토큰은 쓸모가 없다 — 덮어쓴다
    @Test
    fun joiningAnotherRoomReplacesPreviousSession() {
        freshStorage().saveGuestSession(token = "old", roomId = 1L)

        freshStorage().saveGuestSession(token = "new", roomId = 2L)

        val reopened = freshStorage()

        assertEquals("new", reopened.guestToken)
        assertEquals(2L, reopened.guestRoomId)
    }
}
