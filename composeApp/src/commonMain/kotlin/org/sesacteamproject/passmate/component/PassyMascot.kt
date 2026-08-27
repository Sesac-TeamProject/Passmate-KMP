package org.sesacteamproject.passmate.component

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// 마스코트 '패시' — Figma 컴포넌트(172:1037) SVG 지오메트리를 그대로 벡터로 옮김.
// 사용 규칙: Empty State·완료·대기 상태 전용, 문제 풀이 화면 사용 금지 (디자인 시스템 §마스코트)
@Composable
fun PassyMascot(modifier: Modifier = Modifier) {
    Image(
        imageVector = passyMascotVector,
        contentDescription = "패스메이트 마스코트 패시",
        modifier = modifier
    )
}

private const val BODY_YELLOW = 0xFFFFD65E
private const val WING_ORANGE = 0xFFFFB020
private const val ACCENT_ORANGE = 0xFFFF8A5C
private const val EYE_BLACK = 0xFF1B1F24
private const val CHEEK_PINK = 0xFFFFB5B5
private const val HIGHLIGHT_WHITE = 0xFFFFFFFF

private fun androidx.compose.ui.graphics.vector.ImageVector.Builder.ellipse(
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
    color: Long
) {
    path(fill = SolidColor(Color(color))) {
        moveTo(cx - rx, cy)
        arcTo(rx, ry, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = cx + rx, y1 = cy)
        arcTo(rx, ry, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = cx - rx, y1 = cy)
        close()
    }
}

private fun androidx.compose.ui.graphics.vector.ImageVector.Builder.roundedRect(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    radius: Float,
    color: Long
) {
    path(fill = SolidColor(Color(color))) {
        moveTo(x + radius, y)
        lineTo(x + width - radius, y)
        arcTo(radius, radius, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = x + width, y1 = y + radius)
        lineTo(x + width, y + height - radius)
        arcTo(radius, radius, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = x + width - radius, y1 = y + height)
        lineTo(x + radius, y + height)
        arcTo(radius, radius, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = x, y1 = y + height - radius)
        lineTo(x, y + radius)
        arcTo(radius, radius, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = x + radius, y1 = y)
        close()
    }
}

private val passyMascotVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "PassyMascot",
        defaultWidth = 120.dp,
        defaultHeight = 132.dp,
        viewportWidth = 120f,
        viewportHeight = 132f
    ).apply {
        group(rotate = 12f, pivotX = 55.4354f, pivotY = 11.8866f) {
            ellipse(55.4354f, 11.8866f, 5f, 7f, BODY_YELLOW)
        }
        group(rotate = -14f, pivotX = 65.3327f, pivotY = 8.85409f) {
            ellipse(65.3327f, 8.85409f, 4f, 6f, BODY_YELLOW)
        }
        ellipse(60f, 63f, 44f, 47f, BODY_YELLOW)
        group(rotate = -18f, pivotX = 19.8368f, pivotY = 66.2246f) {
            ellipse(19.8368f, 66.2246f, 10f, 14f, WING_ORANGE)
        }
        group(rotate = 18f, pivotX = 99.1843f, pivotY = 72.405f) {
            ellipse(99.1843f, 72.405f, 10f, 14f, WING_ORANGE)
        }
        roundedRect(38f, 106f, 14f, 8f, 4f, ACCENT_ORANGE)
        roundedRect(68f, 106f, 14f, 8f, 4f, ACCENT_ORANGE)
        ellipse(42f, 51f, 5f, 5f, EYE_BLACK)
        ellipse(78f, 51f, 5f, 5f, EYE_BLACK)
        ellipse(41.5f, 49.5f, 1.5f, 1.5f, HIGHLIGHT_WHITE)
        ellipse(77.5f, 49.5f, 1.5f, 1.5f, HIGHLIGHT_WHITE)
        ellipse(34f, 66f, 6f, 4f, CHEEK_PINK)
        ellipse(86f, 66f, 6f, 4f, CHEEK_PINK)
        ellipse(60f, 63f, 7f, 5f, ACCENT_ORANGE)
    }.build()
}
