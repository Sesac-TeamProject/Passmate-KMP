package org.sesacteamproject.passmate.payment.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// M-12-3 은행 드롭다운 — 표준 은행 코드(금융결제원)로 백엔드 bankCode를 채운다
class BankTest {

    @Test
    fun resolvesBankByStandardCode() {
        assertEquals(Bank.KOOKMIN, Bank.fromCode("004"))
        assertEquals(Bank.SHINHAN, Bank.fromCode("088"))
        assertEquals("국민은행", Bank.KOOKMIN.displayName)
    }

    @Test
    fun unknownOrMissingCodeIsNull() {
        assertNull(Bank.fromCode("999"))
        assertNull(Bank.fromCode(null))
    }

    @Test
    fun codesAreUniqueAndListIsNotEmpty() {
        val codes = Bank.all.map { it.code }

        assertTrue(codes.isNotEmpty())
        assertEquals(codes.size, codes.toSet().size)
    }
}
