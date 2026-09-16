package org.sesacteamproject.passmate.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.sesacteamproject.passmate.navigation.AppShellLayout
import org.sesacteamproject.passmate.navigation.AppShellLayoutPolicy
import org.sesacteamproject.passmate.navigation.AppTab
import org.sesacteamproject.passmate.theme.PassmateColors

// 내비게이션과 본문의 배치만 담당하는 셸 — Android·Desktop·iOS가 모두 이걸 거친다 (스펙 2026-09-16 §2).
// 어떤 탭이 켜졌는지(selectedTab)와 눌렀을 때 할 일(onSelectTab)은 호출부가 정한다.
// selectedTab이 null이면 레일도 하단바도 그리지 않는다 — 세션 플로우·SignIn·명성·정산에서
// 바를 숨기는 기존 규칙(규칙 §2-1-1)이 레일에도 그대로 적용된다
@Composable
fun PassmateNavShell(
    selectedTab: AppTab?,
    onSelectTab: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        when (AppShellLayoutPolicy.layoutFor(maxWidth)) {
            AppShellLayout.RAIL -> Row(modifier = Modifier.fillMaxSize()) {
                if (selectedTab != null) {
                    PassmateNavigationRail(
                        selectedTab = selectedTab,
                        onSelectTab = onSelectTab
                    )
                }
                ContentArea(modifier = Modifier.weight(1f), content = content)
            }
            AppShellLayout.BOTTOM_BAR -> Column(modifier = Modifier.fillMaxSize()) {
                ContentArea(modifier = Modifier.weight(1f), content = content)
                if (selectedTab != null) {
                    PassmateBottomTabBar(
                        selectedTab = selectedTab,
                        onSelectTab = onSelectTab
                    )
                }
            }
        }
    }
}

// 본문 — 넓은 창에서 시안 폭을 넘지 않게 묶고 가운데 정렬한다. 남는 좌우는 앱 배경색으로 채운다.
// 화면들이 스스로 흰 Surface를 깔기 때문에 여백까지 흰색이면 본문이 어디까지인지 보이지 않는다
@Composable
private fun ContentArea(
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize().background(PassmateColors.BackgroundMint),
        contentAlignment = Alignment.TopCenter
    ) {
        // widthIn이 먼저 들어오는 제약을 깎고, 그다음 fillMaxSize가 깎인 제약을 채운다.
        // 순서를 바꾸면 fillMaxSize가 원래 제약을 먼저 채워 클램프가 무시된다
        Box(
            modifier = Modifier
                .widthIn(max = AppShellLayoutPolicy.CONTENT_MAX_WIDTH)
                .fillMaxSize()
        ) {
            content()
        }
    }
}
