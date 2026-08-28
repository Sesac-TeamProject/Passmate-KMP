package org.sesacteamproject.passmate.session.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuestionDeadlineTest {

    @Test
    fun computesDurationFromServerTimes() {
        val deadline = QuestionDeadline.fromServerTimes(
            endsAt = "2026-08-27T10:01:30Z",
            serverNow = "2026-08-27T10:01:00Z"
        )

        assertEquals(30_000L, deadline?.totalMillis)
        assertTrue((deadline?.remainingMillis() ?: 0L) <= 30_000L)
        assertTrue((deadline?.remainingSeconds() ?: 0) in 29..30)
    }

    @Test
    fun clampsPastDeadlineToZero() {
        val deadline = QuestionDeadline.fromServerTimes(
            endsAt = "2026-08-27T10:01:00Z",
            serverNow = "2026-08-27T10:01:30Z"
        )

        assertEquals(0L, deadline?.totalMillis)
        assertEquals(0, deadline?.remainingSeconds())
    }

    @Test
    fun rejectsMalformedTimes() {
        assertNull(QuestionDeadline.fromServerTimes("bad", "2026-08-27T10:01:00Z"))
    }
}
