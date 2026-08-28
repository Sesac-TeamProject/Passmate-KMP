package org.sesacteamproject.passmate.user.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PendingGuestClaimTest {

    @Test
    fun consumeReturnsRequestedIdOnce() {
        val pending = PendingGuestClaim()

        pending.request(42L)

        assertEquals(42L, pending.consume())
        // 한 번 소비하면 비워진다 — 중복 claim 방지
        assertNull(pending.consume())
    }

    @Test
    fun consumeReturnsNullWhenNoPending() {
        val pending = PendingGuestClaim()

        assertNull(pending.consume())
    }

    @Test
    fun requestOverwritesPrevious() {
        val pending = PendingGuestClaim()

        pending.request(1L)
        pending.request(2L)

        assertEquals(2L, pending.consume())
    }
}
