package org.sesacteamproject.passmate.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.sesacteamproject.passmate.theme.PassmateColors

// 별점 1~5 (디자인 시스템 §StarRating, 골드 #F2C94C 전용). onSelect가 null이면 읽기 전용.
// 별은 리소스 아이콘으로 그린다 — 예전엔 ★/☆ 글리프였는데 기기 폰트마다 모양이 달라
// 시안과 같아질 수 없었고 규칙 §11-3(화면 코드에 아이콘을 그리지 않는다)에도 어긋났다
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

            PassmateIcon(
                icon = if (isFilled) PassmateIcons.StarFilled else PassmateIcons.Star,
                contentDescription = null,
                tint = if (isFilled) PassmateColors.StarGold else PassmateColors.Border,
                modifier = starModifier.size(starSize.dp)
            )
        }
    }
}
