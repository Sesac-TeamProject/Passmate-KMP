package org.sesacteamproject.passmate.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.navigation.AppTab
import org.sesacteamproject.passmate.theme.PassmateColors

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
                .padding(top = 10.dp, bottom = 0.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            AppTab.entries.forEach { tab ->
                TabItem(
                    tab = tab,
                    isSelected = tab == selectedTab,
                    onClick = { onSelectTab(tab) }
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    tab: AppTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = if (isSelected) PassmateColors.Primary else PassmateColors.TextTertiary

    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
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
