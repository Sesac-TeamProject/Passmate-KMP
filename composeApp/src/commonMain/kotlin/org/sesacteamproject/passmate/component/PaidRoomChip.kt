package org.sesacteamproject.passmate.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.theme.PassmateColors

// 유료 방 표시 칩 "₩ 유료" — 시안 RoomType 컴포넌트(M-11 유료 방 입장 · M-T4 정산 내역 · M-10 프로필 시트).
// iOS PaidRoomChipView와 1:1 미러
@Composable
fun PaidRoomChip(modifier: Modifier = Modifier) {
    Text(
        text = "₩ 유료",
        color = PassmateColors.WeakTopicText,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.28).sp,
        modifier = modifier
            .background(PassmateColors.WeakTopicBg, CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
