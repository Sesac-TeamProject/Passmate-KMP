package org.sesacteamproject.passmate.payment.domain.policy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoinPolicyTest {

    private val policy = CoinPolicy()

    @Test
    fun shortfallIsPositiveWhenBalanceBelowFee() {
        assertEquals(8_800, policy.shortfall(balance = 1_200, entryFee = 10_000))
    }

    @Test
    fun shortfallIsZeroWhenBalanceCoversFee() {
        assertEquals(0, policy.shortfall(balance = 10_000, entryFee = 10_000))
        assertEquals(0, policy.shortfall(balance = 12_000, entryFee = 10_000))
    }

    @Test
    fun hasEnoughReflectsBalance() {
        assertTrue(policy.hasEnough(balance = 5_000, entryFee = 5_000))
        assertFalse(policy.hasEnough(balance = 4_999, entryFee = 5_000))
    }

    @Test
    fun suggestedChargePicksFirstPresetCoveringShortfall() {
        assertEquals(10_000, policy.suggestedChargeAmount(8_800))
        assertEquals(30_000, policy.suggestedChargeAmount(10_001))
    }

    @Test
    fun presetsMatchChargeScreenAmounts() {
        assertEquals(listOf(5_000, 10_000, 30_000, 50_000), policy.presets)
    }

    @Test
    fun suggestedChargePicksSmallestPresetForSmallShortfall() {
        assertEquals(5_000, policy.suggestedChargeAmount(3_000))
    }

    @Test
    fun suggestedChargeFallsBackToShortfallBeyondPresets() {
        assertEquals(80_000, policy.suggestedChargeAmount(80_000))
    }
}
