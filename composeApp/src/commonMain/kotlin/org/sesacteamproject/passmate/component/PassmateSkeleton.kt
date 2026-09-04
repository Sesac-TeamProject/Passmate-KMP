package org.sesacteamproject.passmate.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.sesacteamproject.passmate.theme.PassmateColors

// 시안 "07 · 로딩 · 스켈레톤" 규격 구현. iOS PassmateSkeletonView.swift와 1:1 유지.
//
// 규격 요지:
//  - 블록 색은 SkeletonBlock, 보조는 SkeletonBlockSoft. 민트 계열은 쓰지 않는다.
//  - 텍스트 줄 모서리 6, 카드는 원본과 같은 라운드, 아바타는 원형.
//  - 폭은 실제 글자의 60~90%로 서로 다르게 잡고 마지막 줄은 짧게.
//  - 반짝임은 왼쪽에서 오른쪽으로 1.2초 반복, 흰색 0 → 75% → 0 그라데이션.

private const val SHIMMER_DURATION_MS = 1200

private const val SHIMMER_HIGHLIGHT_ALPHA = 0.75f

// 반짝임 띠 폭 — 블록 폭 대비 비율
private const val SHIMMER_BAND_RATIO = 0.4f

// 텍스트 줄 스켈레톤 기본 모서리
private val TEXT_LINE_RADIUS = 6.dp

// 지나가는 하이라이트 띠. 그리는 대상 위에 얹으므로 clip 뒤에 붙인다.
fun Modifier.passmateShimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "passmateSkeleton")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SHIMMER_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "passmateSkeletonProgress"
    )

    drawWithContent {
        val bandWidth = size.width * SHIMMER_BAND_RATIO
        val travel = size.width + bandWidth * 2
        val bandStart = -bandWidth + travel * progress

        drawContent()
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = SHIMMER_HIGHLIGHT_ALPHA),
                    Color.Transparent
                ),
                start = Offset(bandStart, 0f),
                end = Offset(bandStart + bandWidth, 0f)
            )
        )
    }
}

// 글자 한 줄·칩·버튼 자리를 메우는 블록. 크기는 호출부가 modifier로 준다.
@Composable
fun PassmateSkeletonBlock(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = TEXT_LINE_RADIUS,
    color: Color = PassmateColors.SkeletonBlock
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(color)
            .passmateShimmer()
    )
}

// 카드 자리 — 실제 카드와 같은 테두리·라운드를 유지하고 안쪽만 블록으로 채운다.
@Composable
fun PassmateSkeletonCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(cornerRadius))
            .background(PassmateColors.Surface, RoundedCornerShape(cornerRadius)),
        content = content
    )
}
