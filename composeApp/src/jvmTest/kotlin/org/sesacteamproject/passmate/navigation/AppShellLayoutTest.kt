package org.sesacteamproject.passmate.navigation

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

// 배치 경계 판정 — 스펙 2026-09-16 §1-1
class AppShellLayoutTest {

    @Test
    fun narrowerThanBoundaryUsesBottomBar() {
        assertEquals(AppShellLayout.BOTTOM_BAR, AppShellLayoutPolicy.layoutFor(599.dp))
    }

    @Test
    fun exactlyBoundaryUsesRail() {
        assertEquals(AppShellLayout.RAIL, AppShellLayoutPolicy.layoutFor(600.dp))
    }

    @Test
    fun widerThanBoundaryUsesRail() {
        assertEquals(AppShellLayout.RAIL, AppShellLayoutPolicy.layoutFor(601.dp))
    }

    // 첫 측정 프레임에서 0이 들어와도 레일이 번쩍이지 않아야 한다
    @Test
    fun zeroWidthUsesBottomBar() {
        assertEquals(AppShellLayout.BOTTOM_BAR, AppShellLayoutPolicy.layoutFor(0.dp))
    }

    @Test
    fun boundaryMatchesMaterialCompactBreakpoint() {
        assertEquals(600.dp, AppShellLayoutPolicy.RAIL_MIN_WIDTH)
    }
}
