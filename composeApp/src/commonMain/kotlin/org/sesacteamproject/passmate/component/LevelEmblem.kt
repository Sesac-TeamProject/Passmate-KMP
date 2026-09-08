package org.sesacteamproject.passmate.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import org.sesacteamproject.passmate.room.domain.model.HostLevel
import org.sesacteamproject.passmate.theme.PassmateColors

// T086(US12) 명성 레벨 엠블럼 — 육각형 배경 + 등급별 심볼(새싹·성장·체크·별·왕관).
// 시안 LevelEmblem(M-09·M-10·M-13 실측, 48 기준): 바깥 육각 48은 위→아래 그라디언트(LevelEmblemGradientTop→Primary)에
// 연민트 링 1.5, 안쪽 육각 37.5는 Primary 15%→55% 그라디언트에 흰 28% 선 0.75, 위쪽에 흰 22% 광택 타원 30x13.5.
// Lv.5는 골드 육각(시안 미실측 — 단색 유지). Canvas로 그려 3플랫폼 미러
@Composable
fun LevelEmblem(
    level: HostLevel,
    modifier: Modifier = Modifier
) {
    val isMaster = level == HostLevel.MASTER
    val symbolColor = if (isMaster) PassmateColors.PrimaryDeep else PassmateColors.Surface

    Canvas(modifier = modifier) {
        val r = size.minDimension / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f

        if (isMaster) {
            drawHexagon(cx, cy, r, PassmateColors.StarGold)
        } else {
            drawGradientHexagon(cx, cy, r)
        }
        when (level) {
            HostLevel.SEEDLING -> drawSprout(cx, cy, r, symbolColor, leaves = 2)
            HostLevel.GROWING -> drawSprout(cx, cy, r, symbolColor, leaves = 3)
            HostLevel.VERIFIED -> drawCheck(cx, cy, r, symbolColor)
            HostLevel.POPULAR -> drawStar(cx, cy, r * 0.55f, symbolColor)
            HostLevel.MASTER -> drawCrown(cx, cy, r, symbolColor)
        }
    }
}

private fun hexagonPath(cx: Float, cy: Float, r: Float): Path {
    val path = Path()

    for (i in 0 until 6) {
        val angle = (PI / 180f * (60 * i - 90)).toFloat()
        val x = cx + r * cos(angle)
        val y = cy + r * sin(angle)

        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

// 비율은 전부 시안 48 기준을 반지름(24)으로 나눈 값이다
private fun DrawScope.drawGradientHexagon(cx: Float, cy: Float, r: Float) {
    val outer = hexagonPath(cx, cy, r)
    val inner = hexagonPath(cx, cy, r * 0.78f)
    val outerBrush = Brush.verticalGradient(
        colors = listOf(PassmateColors.LevelEmblemGradientTop, PassmateColors.Primary),
        startY = cy - r,
        endY = cy + r
    )
    val innerBrush = Brush.verticalGradient(
        colors = listOf(PassmateColors.Primary.copy(alpha = 0.15f), PassmateColors.Primary.copy(alpha = 0.55f)),
        startY = cy - r * 0.78f,
        endY = cy + r * 0.78f
    )

    drawPath(outer, outerBrush)
    drawPath(outer, PassmateColors.AchievementBadgeBorder, style = Stroke(width = r * 0.0625f))
    drawPath(inner, innerBrush)
    drawPath(inner, PassmateColors.Surface.copy(alpha = 0.28f), style = Stroke(width = r * 0.03125f))
    drawOval(
        color = PassmateColors.Surface.copy(alpha = 0.22f),
        topLeft = Offset(cx - r * 0.625f, cy - r * 0.78f),
        size = Size(r * 1.25f, r * 0.5625f)
    )
}

private fun DrawScope.drawHexagon(cx: Float, cy: Float, r: Float, color: Color) {
    drawPath(hexagonPath(cx, cy, r), color)
}

private fun DrawScope.drawSprout(cx: Float, cy: Float, r: Float, color: Color, leaves: Int) {
    val stroke = Stroke(width = r * 0.14f, cap = StrokeCap.Round)

    drawLine(color, Offset(cx, cy + r * 0.5f), Offset(cx, cy - r * 0.15f), strokeWidth = r * 0.14f, cap = StrokeCap.Round)
    drawOval(color, topLeft = Offset(cx - r * 0.5f, cy - r * 0.35f), size = Size(r * 0.5f, r * 0.32f))
    drawOval(color, topLeft = Offset(cx, cy - r * 0.35f), size = Size(r * 0.5f, r * 0.32f))
    if (leaves >= 3) {
        drawOval(color, topLeft = Offset(cx - r * 0.22f, cy - r * 0.62f), size = Size(r * 0.44f, r * 0.3f))
    }
}

private fun DrawScope.drawCheck(cx: Float, cy: Float, r: Float, color: Color) {
    val path = Path()

    path.moveTo(cx - r * 0.42f, cy + r * 0.02f)
    path.lineTo(cx - r * 0.1f, cy + r * 0.35f)
    path.lineTo(cx + r * 0.45f, cy - r * 0.35f)
    drawPath(path, color, style = Stroke(width = r * 0.18f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawStar(cx: Float, cy: Float, radius: Float, color: Color) {
    val path = Path()
    val inner = radius * 0.45f

    for (i in 0 until 10) {
        val rad = if (i % 2 == 0) radius else inner
        val angle = (PI / 180f * (36 * i - 90)).toFloat()
        val x = cx + rad * cos(angle)
        val y = cy + rad * sin(angle)

        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

private fun DrawScope.drawCrown(cx: Float, cy: Float, r: Float, color: Color) {
    val path = Path()
    val top = cy - r * 0.4f
    val bottom = cy + r * 0.4f
    val left = cx - r * 0.5f
    val right = cx + r * 0.5f

    path.moveTo(left, bottom)
    path.lineTo(left, top)
    path.lineTo(cx - r * 0.22f, cy + r * 0.05f)
    path.lineTo(cx, top - r * 0.08f)
    path.lineTo(cx + r * 0.22f, cy + r * 0.05f)
    path.lineTo(right, top)
    path.lineTo(right, bottom)
    path.close()
    drawPath(path, color)
}
