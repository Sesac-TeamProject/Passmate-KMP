package org.sesacteamproject.passmate.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import org.sesacteamproject.passmate.theme.PassmateColors

// 시안 v6의 대기 점 3개 — 대기실(M-02)과 문항 결과(M-04)가 같은 것을 쓴다.
// 비활성 점도 같은 민트를 옅게 쓴다 — Border(연회색) x 0.4는 실기기에서 거의 안 보였다
@Composable
fun PassmateWaitingDots(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = DOT_COUNT.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(DOT_COUNT) { index ->
            val isActive = phase.toInt() % DOT_COUNT == index

            Box(
                modifier = Modifier
                    .size(7.dp)
                    .alpha(if (isActive) 1f else INACTIVE_DOT_ALPHA)
                    .background(color = PassmateColors.Primary, shape = CircleShape)
            )
        }
    }
}

private const val DOT_COUNT = 3

private const val INACTIVE_DOT_ALPHA = 0.35f
