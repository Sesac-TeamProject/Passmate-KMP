package org.sesacteamproject.passmate.navigation

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// 셸이 고를 수 있는 내비게이션 배치 (스펙 2026-09-16 §1-1)
enum class AppShellLayout {
    BOTTOM_BAR,
    RAIL
}

// 가로폭 하나로 배치를 정한다. 판정은 여기 한 곳에만 둔다 (규칙 §2-1-1의 barOwnerOf와 같은 원칙)
object AppShellLayoutPolicy {

    // Material3 window size class의 compact/medium 경계
    val RAIL_MIN_WIDTH: Dp = 600.dp

    // 재는 값은 셸이 받은 **창 폭**이다. 레일을 뺀 본문 폭으로 재면 600 근처에서 배치가 튄다
    // (레일이 붙으면 본문이 600 밑으로 내려가 다시 하단바가 되고, 그러면 다시 600을 넘는다)
    fun layoutFor(width: Dp): AppShellLayout {
        return if (width >= RAIL_MIN_WIDTH) AppShellLayout.RAIL else AppShellLayout.BOTTOM_BAR
    }
}
