package org.sesacteamproject.passmate.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.sesacteamproject.passmate.navigation.AppTab
import org.sesacteamproject.passmate.theme.PassmateColors

// 탭바 여백 배분 — 바깥(좌·우) 6, 항목 사이 5 (총 5군데 6:5:5:5:6)
private const val OUTER_GAP_WEIGHT = 6f

private const val INNER_GAP_WEIGHT = 5f

// 하단 4탭 바 (피그마 v6) — Android·Desktop 공용. 탭 루트에서만 표시한다 (스펙 §1-2)
@Composable
fun PassmateBottomTabBar(
    selectedTab: AppTab?,
    onSelectTab: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().background(PassmateColors.Surface).navigationBarsPadding()) {
        Divider(color = PassmateColors.Border, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // 시안(M-01) 탭바는 상단 10 · 항목 49 · 하단 14다. 하단 14는 시스템 바 자리를
                // 대신하는 값이라 실기기에서는 navigationBarsPadding()이 그 역할을 한다 —
                // 여기에 또 얹으면 탭바가 뜬다
                .padding(top = 10.dp, bottom = 0.dp)
        ) {
            // 좌우 바깥 여백과 항목 사이 간격, 다섯 군데를 6:5:5:5:6으로 나눈다. 남는 폭을
            // 비율로 가르므로 라벨 길이나 화면 폭이 달라져도 비율이 그대로 유지된다
            Spacer(modifier = Modifier.weight(OUTER_GAP_WEIGHT))
            AppTab.entries.forEachIndexed { index, tab ->
                val isLast = index == AppTab.entries.lastIndex

                PassmateTabItem(
                    tab = tab,
                    isSelected = tab == selectedTab,
                    onClick = { onSelectTab(tab) }
                )
                Spacer(
                    modifier = Modifier.weight(
                        if (isLast) OUTER_GAP_WEIGHT else INNER_GAP_WEIGHT
                    )
                )
            }
        }
    }
}
