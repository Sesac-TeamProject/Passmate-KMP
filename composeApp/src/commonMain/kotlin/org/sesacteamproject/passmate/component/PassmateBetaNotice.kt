package org.sesacteamproject.passmate.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.theme.PassmateColors

// 베타 안내 배너 — 시안 "13 · 베타 운영"의 banner/베타 안내(1111:9662). BETA 칩 + 제목 한 줄 + 설명.
// 잠긴 버튼 **바로 위**에 둔다: 왜 안 눌리는지를 버튼보다 먼저 읽어야 한다.
// 여러 줄 설명은 body에 개행(\n)으로 준다. iOS PassmateBetaNoticeView와 1:1 미러
@Composable
fun PassmateBetaNotice(
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PassmateColors.BackgroundMint, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BETA",
                color = PassmateColors.Surface,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 14.sp,
                letterSpacing = 0.4.sp,
                modifier = Modifier
                    .background(PassmateColors.Primary, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
            Text(
                text = title,
                color = PassmateColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp,
                letterSpacing = (-0.26).sp
            )
        }
        Text(
            text = body,
            color = PassmateColors.TextSecondary,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            letterSpacing = (-0.24).sp
        )
    }
}
