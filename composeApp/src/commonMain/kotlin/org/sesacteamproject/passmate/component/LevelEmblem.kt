package org.sesacteamproject.passmate.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import org.sesacteamproject.passmate.room.domain.model.HostLevel
import org.sesacteamproject.passmate.theme.PassmateColors

// T086(US12) 명성 레벨 엠블럼 — 육각형 배경 + 등급별 심볼(새싹·성장·체크·별·왕관).
// 디자인 시스템 §LevelEmblem(383:12372). Lv.5는 골드 육각. Canvas로 그려 3플랫폼 미러
@Composable
fun LevelEmblem(
    level: HostLevel,
    modifier: Modifier = Modifier
) {
    val hexColor = if (level == HostLevel.MASTER) PassmateColors.StarGold else PassmateColors.Primary
    val symbolColor = if (level == HostLevel.MASTER) PassmateColors.PrimaryDeep else PassmateColors.Surface

    Canvas(modifier = modifier) {
        val r = size.minDimension / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f

        drawHexagon(cx, cy, r, hexColor)
        when (level) {
            HostLevel.SEEDLING -> drawSprout(cx, cy, r, symbolColor, leaves = 2)
            HostLevel.GROWING -> drawSprout(cx, cy, r, symbolColor, leaves = 3)
            HostLevel.VERIFIED -> drawCheck(cx, cy, r, symbolColor)
            HostLevel.POPULAR -> drawStar(cx, cy, r * 0.55f, symbolColor)
            HostLevel.MASTER -> drawCrown(cx, cy, r, symbolColor)
        }
    }
}

private fun DrawScope.drawHexagon(cx: Float, cy: Float, r: Float, color: androidx.compose.ui.graphics.Color) {
    val path = Path()

    for (i in 0 until 6) {
        val angle = (PI / 180f * (60 * i - 90)).toFloat()
        val x = cx + r * cos(angle)
        val y = cy + r * sin(angle)

        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

private fun DrawScope.drawSprout(cx: Float, cy: Float, r: Float, color: androidx.compose.ui.graphics.Color, leaves: Int) {
    val stroke = Stroke(width = r * 0.14f, cap = StrokeCap.Round)

    drawLine(color, Offset(cx, cy + r * 0.5f), Offset(cx, cy - r * 0.15f), strokeWidth = r * 0.14f, cap = StrokeCap.Round)
    drawOval(color, topLeft = Offset(cx - r * 0.5f, cy - r * 0.35f), size = androidx.compose.ui.geometry.Size(r * 0.5f, r * 0.32f))
    drawOval(color, topLeft = Offset(cx, cy - r * 0.35f), size = androidx.compose.ui.geometry.Size(r * 0.5f, r * 0.32f))
    if (leaves >= 3) {
        drawOval(color, topLeft = Offset(cx - r * 0.22f, cy - r * 0.62f), size = androidx.compose.ui.geometry.Size(r * 0.44f, r * 0.3f))
    }
}

private fun DrawScope.drawCheck(cx: Float, cy: Float, r: Float, color: androidx.compose.ui.graphics.Color) {
    val path = Path()

    path.moveTo(cx - r * 0.42f, cy + r * 0.02f)
    path.lineTo(cx - r * 0.1f, cy + r * 0.35f)
    path.lineTo(cx + r * 0.45f, cy - r * 0.35f)
    drawPath(path, color, style = Stroke(width = r * 0.18f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawStar(cx: Float, cy: Float, radius: Float, color: androidx.compose.ui.graphics.Color) {
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

private fun DrawScope.drawCrown(cx: Float, cy: Float, r: Float, color: androidx.compose.ui.graphics.Color) {
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
