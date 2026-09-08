package org.sesacteamproject.passmate.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.preview.PassmatePreview
import org.sesacteamproject.passmate.theme.PassmateColors
import org.sesacteamproject.passmate.theme.PassmateTheme

// Figma "UI 디자인 v6" M-07(349:9529) 연결 끊김·재접속 — 대기실(M-02)과 풀이(M-03)가 같은 것을 쓴다.
// 흰 바탕 전체 화면 위에 카드 하나(r28·테두리·패딩 36/28/32/28·간격 16). 자동 재연결은 shared가 계속 시도하고,
// 버튼은 백오프를 건너뛰는 즉시 재구독이다. 표시 여부와 생명주기는 호출한 컨테이너 Screen이 소유한다 (규칙 §11-1)
@Composable
fun PassmateDisconnectedOverlay(
    onClickReconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PassmateColors.Surface)
            // 뒤에 깔린 화면으로 터치가 새지 않게 여기서 삼킨다
            .pointerInput(Unit) { detectTapGestures { } }
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 20.dp, top = 64.dp, end = 20.dp, bottom = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PassmateColors.Surface, RoundedCornerShape(28.dp))
                .border(1.dp, PassmateColors.Border, RoundedCornerShape(28.dp))
                .padding(start = 28.dp, top = 36.dp, end = 28.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(PassmateColors.FieldGray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "!",
                    color = PassmateColors.PrimaryDeep,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "연결이 끊겼어요",
                color = PassmateColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.48).sp
            )
            Text(
                text = "네트워크를 확인하고 있어요.\n다시 연결되면 진행 중인 문항으로 돌아가요.",
                color = PassmateColors.TextSecondary,
                fontSize = 14.sp,
                lineHeight = 19.6.sp,
                letterSpacing = (-0.28).sp,
                textAlign = TextAlign.Center
            )
            PassmateWaitingDots()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(PassmateColors.Primary, RoundedCornerShape(14.dp))
                    .clickable(onClick = onClickReconnect),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "지금 다시 연결",
                    color = PassmateColors.Surface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.28).sp
                )
            }
        }
    }
}


// --- Preview ---

@PassmatePreview
@Composable
private fun PassmateDisconnectedOverlayPreview() {
    PassmateTheme {
        PassmateDisconnectedOverlay(onClickReconnect = {})
    }
}
