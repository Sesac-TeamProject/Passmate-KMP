package org.sesacteamproject.passmate.room.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import org.sesacteamproject.passmate.core.model.AppError

// 재입장이 실패해도 게스트 세션은 서버가 기록을 거부할 때만 버린다 — 게스트에게 토큰은 유일한 신분증이다 (규칙 §8)
class GuestSessionDiscardTest {

    @Test
    fun discardsWhenServerRejectsTheRecord() {
        assertEquals(true, shouldDiscardGuestSession(AppError.Unauthorized(serverCode = "TOKEN_EXPIRED")))
        assertEquals(true, shouldDiscardGuestSession(AppError.NotFound(serverCode = "PARTICIPANT_NOT_FOUND")))
        assertEquals(true, shouldDiscardGuestSession(AppError.Gone(serverCode = "ROOM_ENDED")))
    }

    // 잠깐 끊겼다고 버리면 연결이 돌아와 입장할 때 같은 사람이 새 참가자로 또 들어간다
    @Test
    fun keepsOnNetworkAndServerFailures() {
        assertEquals(false, shouldDiscardGuestSession(AppError.NetworkError()))
        assertEquals(false, shouldDiscardGuestSession(AppError.Unknown()))
    }

    // 강퇴 기록을 남겨야 새 입장으로 우회하지 못한다
    @Test
    fun keepsWhenKicked() {
        assertEquals(false, shouldDiscardGuestSession(AppError.PermissionDenied(serverCode = "ACCESS_DENIED")))
    }
}
