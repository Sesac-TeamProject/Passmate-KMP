package org.sesacteamproject.passmate.room.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HostLevelTest {

    @Test
    fun mapsLevelNumberToTier() {
        assertEquals(HostLevel.SEEDLING, HostLevel.from(1))
        assertEquals(HostLevel.VERIFIED, HostLevel.from(3))
        assertEquals(HostLevel.MASTER, HostLevel.from(5))
    }

    @Test
    fun labelsMatchDesignSystem() {
        assertEquals("검증된 운영자", HostLevel.VERIFIED.label)
        assertEquals("마스터", HostLevel.MASTER.label)
    }

    @Test
    fun rejectsUnknownLevel() {
        assertNull(HostLevel.from(0))
        assertNull(HostLevel.from(6))
        assertNull(HostLevel.from(null))
    }
}
