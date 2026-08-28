package org.sesacteamproject.passmate.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.room.domain.model.HostLevel
import org.sesacteamproject.passmate.theme.PassmateColors

// 명성 레벨 뱃지 (디자인 시스템 §ReputationBadge) — "Lv.N {등급명}", 파스텔 배경+진한 잉크.
// 엠블럼 그래픽(방패/별/왕관)은 후속 — 여기선 레벨 숫자 원형 엠블럼으로 대체
@Composable
fun ReputationBadge(
    level: HostLevel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(PassmateColors.ReputationBadgeBg, CircleShape)
            .padding(start = 5.dp, top = 4.dp, end = 10.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(PassmateColors.Primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = level.level.toString(),
                color = PassmateColors.Surface,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = "Lv.${level.level} ${level.label}",
            color = PassmateColors.ReputationBadgeText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp
        )
    }
}
