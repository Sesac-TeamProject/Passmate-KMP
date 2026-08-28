package org.sesacteamproject.passmate.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.theme.PassmateColors

// 별점 1~5 (디자인 시스템 §StarRating, 골드 #F2C94C 전용). onSelect가 null이면 읽기 전용
@Composable
fun StarRating(
    stars: Int,
    modifier: Modifier = Modifier,
    starSize: Int = 34,
    onSelect: ((Int) -> Unit)? = null
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (index in 1..5) {
            val isFilled = index <= stars
            val starModifier = if (onSelect != null) {
                Modifier.clickable { onSelect(index) }
            } else {
                Modifier
            }

            Text(
                text = if (isFilled) "★" else "☆",
                color = if (isFilled) PassmateColors.StarGold else PassmateColors.Border,
                fontSize = starSize.sp,
                fontWeight = FontWeight.Medium,
                modifier = starModifier
            )
        }
    }
}
