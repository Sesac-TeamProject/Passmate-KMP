package org.sesacteamproject.passmate.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IsoTimeTest {

    @Test
    fun parsesUtcInstant() {
        assertEquals(0L, IsoTime.toEpochMillis("1970-01-01T00:00:00Z"))
        assertEquals(1_000L, IsoTime.toEpochMillis("1970-01-01T00:00:01Z"))
        // 2026-08-27T10:01:30Z = epoch 1787824890
        assertEquals(1_787_824_890_000L, IsoTime.toEpochMillis("2026-08-27T10:01:30Z"))
    }

    @Test
    fun parsesFractionalSeconds() {
        assertEquals(1_787_824_890_123L, IsoTime.toEpochMillis("2026-08-27T10:01:30.123Z"))
        assertEquals(1_787_824_890_500L, IsoTime.toEpochMillis("2026-08-27T10:01:30.5Z"))
    }

    @Test
    fun parsesOffset() {
        // KST(+09:00) 19:01:30 = UTC 10:01:30
        assertEquals(1_787_824_890_000L, IsoTime.toEpochMillis("2026-08-27T19:01:30+09:00"))
        assertEquals(1_787_824_890_000L, IsoTime.toEpochMillis("2026-08-27T19:01:30+0900"))
    }

    @Test
    fun rejectsMalformed() {
        assertNull(IsoTime.toEpochMillis("not-a-time"))
        assertNull(IsoTime.toEpochMillis(""))
    }
}
