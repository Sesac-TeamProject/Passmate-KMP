package org.sesacteamproject.passmate.session.domain.policy

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SnapshotPolicyTest {

    private val policy = SnapshotPolicy()

    @Test
    fun discardsFramesBeforeSnapshot() {
        assertTrue(policy.isStaleFrame("2026-08-27T10:00:59Z", "2026-08-27T10:01:00Z"))
    }

    @Test
    fun keepsFramesAtOrAfterSnapshot() {
        assertFalse(policy.isStaleFrame("2026-08-27T10:01:00Z", "2026-08-27T10:01:00Z"))
        assertFalse(policy.isStaleFrame("2026-08-27T10:01:01Z", "2026-08-27T10:01:00Z"))
    }

    @Test
    fun keepsFramesWhenTimestampMalformed() {
        assertFalse(policy.isStaleFrame("bad", "2026-08-27T10:01:00Z"))
    }
}
