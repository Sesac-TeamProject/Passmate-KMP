package org.sesacteamproject.passmate.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.sesacteamproject.passmate.navigation.AppTab
import org.sesacteamproject.passmate.theme.PassmateColors

// Material3 NavigationRail 기본 폭. 오른쪽 1dp 구분선을 포함한 바깥 폭이다
private val RAIL_WIDTH = 80.dp

// 항목 사이 간격 — 하단바의 6:5:5:5:6 비율을 세로로 그대로 쓰면 높이 900dp 창에서 항목이 화면 전체로 흩어진다
private val ITEM_GAP = 8.dp

// 항목 묶음을 바 위 가장자리에서 띄우는 값 — 하단바의 padding(top = 10.dp)와 같은 값을 쓴다
private val ITEM_TOP_PADDING = 10.dp

// 좌측 4탭 레일 (넓은 창) — 하단 탭바를 그대로 세운 형태다. 항목은 PassmateTabItem을 공유하고
// 위에서부터 쌓는다 (스펙 2026-09-16 §3). 표시 여부 판정은 호출부(PassmateNavShell)가 한다
@Composable
fun PassmateNavigationRail(
    selectedTab: AppTab?,
    onSelectTab: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxHeight().width(RAIL_WIDTH)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(PassmateColors.Surface)
                // 레일은 화면 높이를 다 쓰므로 상·하단 시스템 바를 스스로 피한다
                .statusBarsPadding()
                .navigationBarsPadding()
                // 항목은 위에서부터 쌓는다 (사용자 결정 2026-09-16 — 가운데 정렬을 뒤집음)
                .padding(top = ITEM_TOP_PADDING),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ITEM_GAP)
        ) {
            AppTab.entries.forEach { tab ->
                PassmateTabItem(
                    tab = tab,
                    isSelected = tab == selectedTab,
                    onClick = { onSelectTab(tab) }
                )
            }
        }
        // 하단바의 위쪽 1dp Divider를 90° 돌린 것. CMP 1.5.12에는 VerticalDivider가 없어 Box로 그린다
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(PassmateColors.Border)
        )
    }
}
