package org.sesacteamproject.passmate.ui.play

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.component.PassyMascot
import org.sesacteamproject.passmate.navigation.NavigationAction
import org.sesacteamproject.passmate.theme.PassmateColors

// 임시 셸 화면 — 본 구현은 T049(실시간 풀이 플로우)에서 M-03 v6 디자인으로 대체된다.
// SESSION_STARTED 수신 시 대기실이 이 라우트로 전환한다 (규칙 §2-1-2 서버 이벤트 전환)
@Composable
fun PlayScreen(
    pin: String,
    onNavigate: (NavigationAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PassmateColors.Surface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PassyMascot(modifier = Modifier.size(width = 80.dp, height = 88.dp))
        Text(
            text = "곧 문제가 시작돼요!",
            color = PassmateColors.TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "풀이 화면은 준비 중이에요",
            color = PassmateColors.TextSecondary,
            fontSize = 14.sp,
            letterSpacing = (-0.28).sp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
