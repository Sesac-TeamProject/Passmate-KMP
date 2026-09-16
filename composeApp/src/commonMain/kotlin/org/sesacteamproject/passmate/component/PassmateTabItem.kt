package org.sesacteamproject.passmate.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.navigation.AppTab
import org.sesacteamproject.passmate.theme.PassmateColors

// 탭 항목 하나 — 하단 탭바(PassmateBottomTabBar)와 좌측 레일(PassmateNavigationRail)이 공유한다 (규칙 §11)
@Composable
fun PassmateTabItem(
    tab: AppTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (isSelected) PassmateColors.Primary else PassmateColors.TextTertiary

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            // 좌우 여백을 두면 하단바의 안쪽 간격에만 24가 더해져 6:5:5:5:6이 어긋난다 — 여백은 전부 부모가 쥔다
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PassmateIcon(
            icon = iconFor(tab),
            contentDescription = tab.label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = tab.label,
            color = color,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = (-0.22).sp
        )
    }
}

// 시안 v6 nav/4탭의 icon/* 과 1:1 (규칙 §11-3 — 화면 코드에 지오메트리를 쓰지 않는다)
private fun iconFor(tab: AppTab): PassmateIcons {
    return when (tab) {
        AppTab.HOME -> PassmateIcons.Home
        AppTab.HOSTED_ROOMS -> PassmateIcons.PlusSquare
        AppTab.JOINED_ROOMS -> PassmateIcons.DoorOpen
        AppTab.MY_INFO -> PassmateIcons.User
    }
}
