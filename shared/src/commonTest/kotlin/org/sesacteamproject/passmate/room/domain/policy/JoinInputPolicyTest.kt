package org.sesacteamproject.passmate.room.domain.policy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JoinInputPolicyTest {

    private val policy = JoinInputPolicy()

    @Test
    fun validPinIsSixDigits() {
        assertTrue(policy.isValidPin("482913"))
        assertFalse(policy.isValidPin("48291"))
        assertFalse(policy.isValidPin("4829133"))
        assertFalse(policy.isValidPin("48291a"))
        assertFalse(policy.isValidPin(""))
    }

    @Test
    fun validNicknameIsTrimmedAndBounded() {
        assertTrue(policy.isValidNickname("준영"))
        assertTrue(policy.isValidNickname("  준영  "))
        assertTrue(policy.isValidNickname("가".repeat(12)))
        assertFalse(policy.isValidNickname(""))
        assertFalse(policy.isValidNickname("   "))
        assertFalse(policy.isValidNickname("가".repeat(13)))
    }

    @Test
    fun extractPinFromQueryParameter() {
        assertEquals("482913", policy.extractPin("https://passmate.app/join?pin=482913"))
        assertEquals("482913", policy.extractPin("passmate://join?room=1&pin=482913"))
    }

    @Test
    fun extractPinFromPlainText() {
        assertEquals("482913", policy.extractPin("482913"))
        assertEquals("482913", policy.extractPin("PIN 482913 으로 입장"))
    }

    @Test
    fun extractPinRejectsLongerDigitRuns() {
        assertNull(policy.extractPin("4829131"))
        assertNull(policy.extractPin("no pin here"))
    }
}
