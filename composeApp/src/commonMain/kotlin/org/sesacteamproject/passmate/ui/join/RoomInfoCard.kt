package org.sesacteamproject.passmate.ui.join

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.component.ReputationBadge
import org.sesacteamproject.passmate.room.domain.model.HostLevel
import org.sesacteamproject.passmate.room.domain.model.RoomInfo
import org.sesacteamproject.passmate.theme.PassmateColors

// T081(US11)/T086(US12): 입장 전 방 정보 — 제목·호스트 등급 뱃지·평균 별점·평가 수 (M-01 입장 전).
// JoinScreen의 삽입 슬롯에 붙는다 (분담 접점 ①). 별점·평가 수는 GET /rooms/pin/{pin}의 host에서 온다
@Composable
fun RoomInfoCard(
    room: RoomInfo,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(16.dp))
            .background(PassmateColors.Surface, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = room.title,
            color = PassmateColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.32).sp
        )
        val host = room.host

        if (host != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${host.nickname} 선생님",
                    color = PassmateColors.TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.28).sp
                )
                val level = HostLevel.from(host.level)

                if (level != null) {
                    ReputationBadge(level = level)
                }
            }
            RatingRow(
                avgStars = host.avgStars,
                ratingCount = host.ratingCount
            )
        }
        MetaRow(room = room)
    }
}

@Composable
private fun RatingRow(
    avgStars: Double?,
    ratingCount: Int?
) {
    if (avgStars != null && (ratingCount ?: 0) > 0) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "★",
                color = PassmateColors.StarGold,
                fontSize = 14.sp
            )
            Text(
                text = "${formatStars(avgStars)} · 평가 ${ratingCount}개",
                color = PassmateColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp
            )
        }
    } else {
        Text(
            text = "아직 평가가 없어요",
            color = PassmateColors.TextTertiary,
            fontSize = 13.sp,
            letterSpacing = (-0.26).sp
        )
    }
}

@Composable
private fun MetaRow(room: RoomInfo) {
    val parts = mutableListOf<String>()

    room.questionCount?.let { parts.add("${it}문항") }
    room.estimatedMinutes?.let { parts.add("약 ${it}분") }
    if (room.isPaid) {
        room.entryFee?.let { parts.add("참가비 ${it}코인") }
    }
    if (parts.isNotEmpty()) {
        Text(
            text = parts.joinToString(" · "),
            color = PassmateColors.TextSecondary,
            fontSize = 12.sp,
            letterSpacing = (-0.24).sp
        )
    }
}

private fun formatStars(avgStars: Double): String {
    val rounded = (avgStars * 10).toLong()

    return "${rounded / 10}.${rounded % 10}"
}
