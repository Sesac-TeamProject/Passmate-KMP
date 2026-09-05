package org.sesacteamproject.passmate.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.theme.PassmateColors

// 시안 v6 M-03 풀이 · M-T2 진행 리모컨의 남은 시간 표시.
// 시계 아이콘 + mm : ss + "남은 시간" 한 줄, 그 아래 진행 바.
// 남은 시간은 서버 endsAt 기준 값을 렌더링만 한다 — 여기서 판정하지 않는다 (규칙 §5)
@Composable
fun PassmateTimerBar(
    remainingSeconds: Int,
    totalSeconds: Int,
    modifier: Modifier = Modifier
) {
    val safeRemaining = remainingSeconds.coerceAtLeast(0)
    val progress = if (totalSeconds > 0) {
        (safeRemaining.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PassmateIcon(
                icon = PassmateIcons.Clock,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = PassmateColors.TimerAmber
            )
            Text(
                text = formatRemaining(safeRemaining),
                color = PassmateColors.TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.padding(start = 10.dp)
            )
            Box(modifier = Modifier.weight(1f))
            Text(
                text = "남은 시간",
                color = PassmateColors.TextSecondary,
                fontSize = 14.sp,
                letterSpacing = (-0.28).sp
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(PassmateColors.TimerTrack, RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(8.dp)
                    .background(PassmateColors.TimerAmber, RoundedCornerShape(4.dp))
            )
        }
    }
}

// mm : ss — 시안 표기(공백 있는 콜론)를 그대로 따른다
private fun formatRemaining(seconds: Int): String {
    val minutes = seconds / 60
    val rest = seconds % 60

    return "${minutes.toString().padStart(2, '0')} : ${rest.toString().padStart(2, '0')}"
}
