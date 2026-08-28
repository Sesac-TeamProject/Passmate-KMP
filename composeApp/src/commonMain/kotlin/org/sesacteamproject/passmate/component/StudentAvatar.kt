package org.sesacteamproject.passmate.component

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// 학생 신원 식별용 아바타 12종 — Figma Avatar 컴포넌트(172:1354) SVG 지오메트리를 그대로 벡터로 옮김.
// 사용 규칙: 항상 닉네임 라벨과 함께 배치, 아바타 단독으로 사람을 식별하지 말 것 (디자인 시스템 §Avatar)
object StudentAvatars {

    const val COUNT = 12

    const val DEFAULT_ID = 1

    val ids: List<Int> = (1..COUNT).toList()

    fun nameOf(avatarId: Int): String {
        return names[avatarId] ?: names.getValue(DEFAULT_ID)
    }

    private val names = mapOf(
        1 to "고양이", 2 to "강아지", 3 to "곰", 4 to "판다", 5 to "토끼", 6 to "여우",
        7 to "개구리", 8 to "펭귄", 9 to "부엉이", 10 to "호랑이", 11 to "너구리", 12 to "공룡"
    )
}

@Composable
fun StudentAvatar(
    avatarId: Int?,
    modifier: Modifier = Modifier
) {
    val resolvedId = if (avatarId != null && avatarId in 1..StudentAvatars.COUNT) {
        avatarId
    } else {
        StudentAvatars.DEFAULT_ID
    }

    Image(
        imageVector = avatarVector(resolvedId),
        contentDescription = "캐릭터 ${StudentAvatars.nameOf(resolvedId)}",
        modifier = modifier
    )
}

private sealed interface AvatarShape {

    data class Oval(
        val cx: Float,
        val cy: Float,
        val rx: Float,
        val ry: Float,
        val color: Long
    ) : AvatarShape

    data class Polygon(
        val points: FloatArray,
        val color: Long
    ) : AvatarShape
}

private val vectorCache = mutableMapOf<Int, ImageVector>()

private fun avatarVector(avatarId: Int): ImageVector {
    return vectorCache.getOrPut(avatarId) {
        val builder = ImageVector.Builder(
            name = "StudentAvatar$avatarId",
            defaultWidth = 36.dp,
            defaultHeight = 36.dp,
            viewportWidth = 36f,
            viewportHeight = 36f
        )

        avatarShapes.getValue(avatarId).forEach { shape ->
            when (shape) {
                is AvatarShape.Oval -> builder.oval(shape)
                is AvatarShape.Polygon -> builder.polygon(shape)
            }
        }
        builder.build()
    }
}

private fun ImageVector.Builder.oval(shape: AvatarShape.Oval) {
    path(fill = SolidColor(Color(shape.color))) {
        moveTo(shape.cx - shape.rx, shape.cy)
        arcTo(shape.rx, shape.ry, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = shape.cx + shape.rx, y1 = shape.cy)
        arcTo(shape.rx, shape.ry, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = shape.cx - shape.rx, y1 = shape.cy)
        close()
    }
}

private fun ImageVector.Builder.polygon(shape: AvatarShape.Polygon) {
    path(fill = SolidColor(Color(shape.color))) {
        moveTo(shape.points[0], shape.points[1])
        for (index in 2 until shape.points.size step 2) {
            lineTo(shape.points[index], shape.points[index + 1])
        }
        close()
    }
}

private val avatarShapes: Map<Int, List<AvatarShape>> = mapOf(
    // 1: 고양이
    1 to listOf(
        AvatarShape.Oval(18f, 18f, 18f, 18f, 0xFFFFF1DE),
        AvatarShape.Polygon(floatArrayOf(10.0909f, 3.2727f, 14.5785f, 10.6364f, 5.6033f, 10.6364f, 10.0909f, 3.2727f), 0xFFFF9F43),
        AvatarShape.Polygon(floatArrayOf(25.9091f, 3.2727f, 30.3967f, 10.6364f, 21.4215f, 10.6364f, 25.9091f, 3.2727f), 0xFFFF9F43),
        AvatarShape.Polygon(floatArrayOf(10.0909f, 6.5454f, 12.4528f, 10.2273f, 7.729f, 10.2273f, 10.0909f, 6.5454f), 0xFFFFD5B0),
        AvatarShape.Polygon(floatArrayOf(25.9091f, 6.5454f, 28.271f, 10.2273f, 23.5472f, 10.2273f, 25.9091f, 6.5454f), 0xFFFFD5B0),
        AvatarShape.Oval(18f, 19.6364f, 13.0909f, 13.0909f, 0xFFFF9F43),
        AvatarShape.Oval(12.8182f, 18.5455f, 4.0909f, 4.3636f, 0xFFFFFFFF),
        AvatarShape.Oval(23.1818f, 18.5455f, 4.0909f, 4.3636f, 0xFFFFFFFF),
        AvatarShape.Oval(12.8182f, 19.3636f, 2.1818f, 2.4545f, 0xFF2D2C30),
        AvatarShape.Oval(23.1818f, 19.3636f, 2.1818f, 2.4545f, 0xFF2D2C30),
        AvatarShape.Oval(12.2727f, 18.5455f, 0.8182f, 0.8182f, 0xFFFFFFFF),
        AvatarShape.Oval(22.6364f, 18.5455f, 0.8182f, 0.8182f, 0xFFFFFFFF),
        AvatarShape.Oval(18f, 25.0909f, 5.1818f, 3.8182f, 0xFFFFE9D2),
        AvatarShape.Oval(18f, 23.1818f, 1.9091f, 1.3636f, 0xFFE8663D),
        AvatarShape.Oval(18f, 26.4545f, 1.6364f, 1.3636f, 0xFF2D2C30),
        AvatarShape.Oval(7.9091f, 24.5455f, 2.7273f, 1.6364f, 0xFFFF8080),
        AvatarShape.Oval(28.0909f, 24.5455f, 2.7273f, 1.6364f, 0xFFFF8080)
    ),
    // 2: 강아지
    2 to listOf(
        AvatarShape.Oval(18f, 18f, 18f, 18f, 0xFFE4F0FF),
        AvatarShape.Oval(10.3636f, 9.8182f, 5.4546f, 5.4546f, 0xFFB98A5E),
        AvatarShape.Oval(25.6364f, 9.8182f, 5.4546f, 5.4546f, 0xFFB98A5E),
        AvatarShape.Oval(18f, 19.6364f, 13.0909f, 13.0909f, 0xFFE3BC93),
        AvatarShape.Oval(12.8182f, 18.5455f, 4.0909f, 4.3636f, 0xFFFFFFFF),
        AvatarShape.Oval(23.1818f, 18.5455f, 4.0909f, 4.3636f, 0xFFFFFFFF),
        AvatarShape.Oval(12.8182f, 19.3636f, 2.1818f, 2.4545f, 0xFF2D2C30),
        AvatarShape.Oval(23.1818f, 19.3636f, 2.1818f, 2.4545f, 0xFF2D2C30),
        AvatarShape.Oval(12.2727f, 18.5455f, 0.8182f, 0.8182f, 0xFFFFFFFF),
        AvatarShape.Oval(22.6364f, 18.5455f, 0.8182f, 0.8182f, 0xFFFFFFFF),
        AvatarShape.Oval(18f, 25.0909f, 5.1818f, 3.8182f, 0xFFF6E6D2),
        AvatarShape.Oval(18f, 23.1818f, 1.9091f, 1.3636f, 0xFF4A3A2E),
        AvatarShape.Oval(18f, 26.4545f, 1.6364f, 1.3636f, 0xFF2D2C30),
        AvatarShape.Oval(7.9091f, 24.5455f, 2.7273f, 1.6364f, 0xFFF09A9A),
        AvatarShape.Oval(28.0909f, 24.5455f, 2.7273f, 1.6364f, 0xFFF09A9A)
    ),
    // 3: 곰
    3 to listOf(
        AvatarShape.Oval(18f, 18f, 18f, 18f, 0xFFFBECD8),
        AvatarShape.Oval(10.3636f, 9.8182f, 5.4546f, 5.4546f, 0xFF96603A),
        AvatarShape.Oval(25.6364f, 9.8182f, 5.4546f, 5.4546f, 0xFF96603A),
        AvatarShape.Oval(10.3636f, 9.8182f, 3f, 3f, 0xFFD9A47C),
        AvatarShape.Oval(25.6364f, 9.8182f, 3f, 3f, 0xFFD9A47C),
        AvatarShape.Oval(18f, 19.6364f, 13.0909f, 13.0909f, 0xFFB5764A),
        AvatarShape.Oval(12.8182f, 18.5455f, 4.0909f, 4.3636f, 0xFFFFFFFF),
        AvatarShape.Oval(23.1818f, 18.5455f, 4.0909f, 4.3636f, 0xFFFFFFFF),
        AvatarShape.Oval(12.8182f, 19.3636f, 2.1818f, 2.4545f, 0xFF2D2C30),
        AvatarShape.Oval(23.1818f, 19.3636f, 2.1818f, 2.4545f, 0xFF2D2C30),
        AvatarShape.Oval(12.2727f, 18.5455f, 0.8182f, 0.8182f, 0xFFFFFFFF),
        AvatarShape.Oval(22.6364f, 18.5455f, 0.8182f, 0.8182f, 0xFFFFFFFF),
        AvatarShape.Oval(18f, 25.0909f, 5.1818f, 3.8182f, 0xFFEBD3B8),
        AvatarShape.Oval(18f, 23.1818f, 1.9091f, 1.3636f, 0xFF3B2A20),
        AvatarShape.Oval(18f, 26.4545f, 1.6364f, 1.3636f, 0xFF2D2C30),
        AvatarShape.Oval(7.9091f, 24.5455f, 2.7273f, 1.6364f, 0xFFD96B6B),
        AvatarShape.Oval(28.0909f, 24.5455f, 2.7273f, 1.6364f, 0xFFD96B6B)
    ),
    // 4: 판다
    4 to listOf(
        AvatarShape.Oval(18f, 18f, 18f, 18f, 0xFFB9E4F5),
        AvatarShape.Oval(10.3636f, 9.8182f, 5.4546f, 5.4546f, 0xFF2D2C30),
        AvatarShape.Oval(25.6364f, 9.8182f, 5.4546f, 5.4546f, 0xFF2D2C30),
        AvatarShape.Oval(18f, 19.6364f, 13.0909f, 13.0909f, 0xFFFFFFFF),
        AvatarShape.Oval(12.5455f, 18.8182f, 4.9091f, 5.1818f, 0xFF2D2C30),
        AvatarShape.Oval(23.4545f, 18.8182f, 4.9091f, 5.1818f, 0xFF2D2C30),
        AvatarShape.Oval(12.8182f, 18.5455f, 4.0909f, 4.3636f, 0xFFFFFFFF),
        AvatarShape.Oval(23.1818f, 18.5455f, 4.0909f, 4.3636f, 0xFFFFFFFF),
        AvatarShape.Oval(12.8182f, 19.3636f, 2.1818f, 2.4545f, 0xFF2D2C30),
        AvatarShape.Oval(23.1818f, 19.3636f, 2.1818f, 2.4545f, 0xFF2D2C30),
        AvatarShape.Oval(12.2727f, 18.5455f, 0.8182f, 0.8182f, 0xFFFFFFFF),
        AvatarShape.Oval(22.6364f, 18.5455f, 0.8182f, 0.8182f, 0xFFFFFFFF),
        AvatarShape.Oval(18f, 25.0909f, 5.1818f, 3.8182f, 0xFFFFFFFF),
        AvatarShape.Oval(18f, 23.1818f, 1.9091f, 1.3636f, 0xFF2D2C30),
        AvatarShape.Oval(18f, 26.4545f, 1.6364f, 1.3636f, 0xFF2D2C30),
        AvatarShape.Oval(7.9091f, 24.5455f, 2.7273f, 1.6364f, 0xFFFFB5C0),
        AvatarShape.Oval(28.0909f, 24.5455f, 2.7273f, 1.6364f, 0xFFFFB5C0)
    ),
    // 5: 토끼
    5 to listOf(
        AvatarShape.Oval(18f, 18f, 18f, 18f, 0xFFF7C8DA),
        AvatarShape.Oval(11.4545f, 6.8182f, 2.7273f, 6.8182f, 0xFFFFFFFF),
        AvatarShape.Oval(24.5455f, 6.8182f, 2.7273f, 6.8182f, 0xFFFFFFFF),
        AvatarShape.Oval(11.4545f, 7.0909f, 1.3636f, 4.3636f, 0xFFF79FB8),
        AvatarShape.Oval(24.5455f, 7.0909f, 1.3636f, 4.3636f, 0xFFF79FB8),
        AvatarShape.Oval(18f, 19.6364f, 13.0909f, 13.0909f, 0xFFFFFFFF),
        AvatarShape.Oval(12.8182f, 18.5455f, 4.0909f, 4.3636f, 0xFFFFFFFF),
        AvatarShape.Oval(23.1818f, 18.5455f, 4.0909f, 4.3636f, 0xFFFFFFFF),
        AvatarShape.Oval(12.8182f, 19.3636f, 2.1818f, 2.4545f, 0xFF2D2C30),
        AvatarShape.Oval(23.1818f, 19.3636f, 2.1818f, 2.4545f, 0xFF2D2C30),
        AvatarShape.Oval(12.2727f, 18.5455f, 0.8182f, 0.8182f, 0xFFFFFFFF),
        AvatarShape.Oval(22.6364f, 18.5455f, 0.8182f, 0.8182f, 0xFFFFFFFF),
        AvatarShape.Oval(18f, 25.0909f, 5.1818f, 3.8182f, 0xFFFFFFFF),
        AvatarShape.Oval(18f, 23.1818f, 1.9091f, 1.3636f, 0xFFEE7C93),
        AvatarShape.Oval(18f, 26.4545f, 1.6364f, 1.3636f, 0xFF2D2C30),
        AvatarShape.Oval(7.9091f, 24.5455f, 2.7273f, 1.6364f, 0xFFF7B9C8),
        AvatarShape.Oval(28.0909f, 24.5455f, 2.7273f, 1.6364f, 0xFFF7B9C8)
    ),
    // 6: 여우
    6 to listOf(
        AvatarShape.Oval(18f, 18f, 18f, 18f, 0xFFFFEDE2),
        AvatarShape.Polygon(floatArrayOf(10.0909f, 3.2727f, 14.5785f, 10.6364f, 5.6033f, 10.6364f, 10.0909f, 3.2727f), 0xFFF0603C),
        AvatarShape.Polygon(floatArrayOf(25.9091f, 3.2727f, 30.3967f, 10.6364f, 21.4215f, 10.6364f, 25.9091f, 3.2727f), 0xFFF0603C),
        AvatarShape.Polygon(floatArrayOf(10.0909f, 6.5454f, 12.4528f, 10.2273f, 7.729f, 10.2273f, 10.0909f, 6.5454f), 0xFFFFD2C2),
        AvatarShape.Polygon(floatArrayOf(25.9091f, 6.5454f, 28.271f, 10.2273f, 23.5472f, 10.2273f, 25.9091f, 6.5454f), 0xFFFFD2C2),
        AvatarShape.Oval(18f, 19.6364f, 13.0909f, 13.0909f, 0xFFF0603C),
        AvatarShape.Oval(18f, 26.1818f, 8.1818f, 5.4546f, 0xFFFFF3EC),
        AvatarShape.Oval(12.8182f, 18.5455f, 4.0909f, 4.3636f, 0xFFFFFFFF),
        AvatarShape.Oval(23.1818f, 18.5455f, 4.0909f, 4.3636f, 0xFFFFFFFF),
        AvatarShape.Oval(12.8182f, 19.3636f, 2.1818f, 2.4545f, 0xFF2D2C30),
        AvatarShape.Oval(23.1818f, 19.3636f, 2.1818f, 2.4545f, 0xFF2D2C30),
        AvatarShape.Oval(12.2727f, 18.5455f, 0.8182f, 0.8182f, 0xFFFFFFFF),
        AvatarShape.Oval(22.6364f, 18.5455f, 0.8182f, 0.8182f, 0xFFFFFFFF),
        AvatarShape.Oval(18f, 25.0909f, 5.1818f, 3.8182f, 0xFFFFF3EC),
        AvatarShape.Oval(18f, 23.1818f, 1.9091f, 1.3636f, 0xFF3B2A20),
        AvatarShape.Oval(18f, 26.4545f, 1.6364f, 1.3636f, 0xFF2D2C30),
        AvatarShape.Oval(7.9091f, 24.5455f, 2.7273f, 1.6364f, 0xFFFF9E86),
        AvatarShape.Oval(28.0909f, 24.5455f, 2.7273f, 1.6364f, 0xFFFF9E86)
    ),
    // 7: 개구리
    7 to listOf(
        AvatarShape.Oval(18f, 18f, 18f, 18f, 0xFFE1F7EA),
        AvatarShape.Oval(18f, 19.6364f, 13.0909f, 13.0909f, 0xFF4CC77F),
        AvatarShape.Oval(12.8182f, 18.5455f, 5.4546f, 5.7273f, 0xFFFFFFFF),
        AvatarShape.Oval(23.1818f, 18.5455f, 5.4546f, 5.7273f, 0xFFFFFFFF),
        AvatarShape.Oval(12.8182f, 19.3636f, 2.1818f, 2.4545f, 0xFF2D2C30),
        AvatarShape.Oval(23.1818f, 19.3636f, 2.1818f, 2.4545f, 0xFF2D2C30),
        AvatarShape.Oval(12.2727f, 18.5455f, 0.8182f, 0.8182f, 0xFFFFFFFF),
        AvatarShape.Oval(22.6364f, 18.5455f, 0.8182f, 0.8182f, 0xFFFFFFFF),
        AvatarShape.Oval(7.9091f, 24.5455f, 2.7273f, 1.6364f, 0xFF3EA96A),
        AvatarShape.Oval(28.0909f, 24.5455f, 2.7273f, 1.6364f, 0xFF3EA96A)
    ),
    // 8: 펭귄
    8 to listOf(
        AvatarShape.Oval(18f, 18f, 18f, 18f, 0xFFCFE8F7),
        AvatarShape.Oval(18f, 19.6364f, 13.0909f, 13.0909f, 0xFF3C4A63),
        AvatarShape.Oval(18f, 24f, 9.2727f, 8.7273f, 0xFFFFFFFF),
        AvatarShape.Oval(12.8182f, 18.5455f, 4.0909f, 4.3636f, 0xFFFFFFFF),
        AvatarShape.Oval(23.1818f, 18.5455f, 4.0909f, 4.3636f, 0xFFFFFFFF),
        AvatarShape.Oval(12.8182f, 19.3636f, 2.1818f, 2.4545f, 0xFF2D2C30),
        AvatarShape.Oval(23.1818f, 19.3636f, 2.1818f, 2.4545f, 0xFF2D2C30),
        AvatarShape.Oval(12.2727f, 18.5455f, 0.8182f, 0.8182f, 0xFFFFFFFF),
        AvatarShape.Oval(22.6364f, 18.5455f, 0.8182f, 0.8182f, 0xFFFFFFFF),
        AvatarShape.Oval(7.9091f, 24.5455f, 2.7273f, 1.6364f, 0xFFF6A6B4),
        AvatarShape.Oval(28.0909f, 24.5455f, 2.7273f, 1.6364f, 0xFFF6A6B4)
    ),
    // 9: 부엉이
    9 to listOf(
        AvatarShape.Oval(18f, 18f, 18f, 18f, 0xFFF6E7D3),
        AvatarShape.Polygon(floatArrayOf(10.3636f, 3.8182f, 14.1427f, 9.9545f, 6.5846f, 9.9545f, 10.3636f, 3.8182f), 0xFFC08552),
        AvatarShape.Polygon(floatArrayOf(25.6364f, 3.8182f, 29.4154f, 9.9545f, 21.8573f, 9.9545f, 25.6364f, 3.8182f), 0xFFC08552),
        AvatarShape.Oval(18f, 19.6364f, 13.0909f, 13.0909f, 0xFFC08552),
        AvatarShape.Oval(12.8182f, 18.5455f, 5.4546f, 5.7273f, 0xFFFFFFFF),
        AvatarShape.Oval(23.1818f, 18.5455f, 5.4546f, 5.7273f, 0xFFFFFFFF),
        AvatarShape.Oval(12.8182f, 19.3636f, 2.1818f, 2.4545f, 0xFF2D2C30),
        AvatarShape.Oval(23.1818f, 19.3636f, 2.1818f, 2.4545f, 0xFF2D2C30),
        AvatarShape.Oval(12.2727f, 18.5455f, 0.8182f, 0.8182f, 0xFFFFFFFF),
        AvatarShape.Oval(22.6364f, 18.5455f, 0.8182f, 0.8182f, 0xFFFFFFFF),
        AvatarShape.Polygon(floatArrayOf(18f, 27.2727f, 15.6381f, 24f, 20.3619f, 24f, 18f, 27.2727f), 0xFFFF9F2E),
        AvatarShape.Oval(7.9091f, 24.5455f, 2.7273f, 1.6364f, 0xFFD98A6A),
        AvatarShape.Oval(28.0909f, 24.5455f, 2.7273f, 1.6364f, 0xFFD98A6A)
    ),
    // 10: 호랑이
    10 to listOf(
        AvatarShape.Oval(18f, 18f, 18f, 18f, 0xFFE9F0D8),
        AvatarShape.Polygon(floatArrayOf(10.0909f, 3.2727f, 14.5785f, 10.6364f, 5.6033f, 10.6364f, 10.0909f, 3.2727f), 0xFFFFB03A),
        AvatarShape.Polygon(floatArrayOf(25.9091f, 3.2727f, 30.3967f, 10.6364f, 21.4215f, 10.6364f, 25.9091f, 3.2727f), 0xFFFFB03A),
        AvatarShape.Polygon(floatArrayOf(10.0909f, 6.5454f, 12.4528f, 10.2273f, 7.729f, 10.2273f, 10.0909f, 6.5454f), 0xFFFFDCA8),
        AvatarShape.Polygon(floatArrayOf(25.9091f, 6.5454f, 28.271f, 10.2273f, 23.5472f, 10.2273f, 25.9091f, 6.5454f), 0xFFFFDCA8),
        AvatarShape.Oval(18f, 19.6364f, 13.0909f, 13.0909f, 0xFFFFB03A),
        AvatarShape.Oval(12.8182f, 18.5455f, 4.0909f, 4.3636f, 0xFFFFFFFF),
        AvatarShape.Oval(23.1818f, 18.5455f, 4.0909f, 4.3636f, 0xFFFFFFFF),
        AvatarShape.Oval(12.8182f, 19.3636f, 2.1818f, 2.4545f, 0xFF2D2C30),
        AvatarShape.Oval(23.1818f, 19.3636f, 2.1818f, 2.4545f, 0xFF2D2C30),
        AvatarShape.Oval(12.2727f, 18.5455f, 0.8182f, 0.8182f, 0xFFFFFFFF),
        AvatarShape.Oval(22.6364f, 18.5455f, 0.8182f, 0.8182f, 0xFFFFFFFF),
        AvatarShape.Oval(18f, 25.0909f, 5.1818f, 3.8182f, 0xFFFFF0D6),
        AvatarShape.Oval(18f, 23.1818f, 1.9091f, 1.3636f, 0xFF3B2A20),
        AvatarShape.Oval(18f, 26.4545f, 1.6364f, 1.3636f, 0xFF2D2C30),
        AvatarShape.Oval(7.9091f, 24.5455f, 2.7273f, 1.6364f, 0xFFF58A6A),
        AvatarShape.Oval(28.0909f, 24.5455f, 2.7273f, 1.6364f, 0xFFF58A6A)
    ),
    // 11: 너구리
    11 to listOf(
        AvatarShape.Oval(18f, 18f, 18f, 18f, 0xFFF3F4F6),
        AvatarShape.Polygon(floatArrayOf(10.0909f, 3.2727f, 14.5785f, 10.6364f, 5.6033f, 10.6364f, 10.0909f, 3.2727f), 0xFF9AA3B5),
        AvatarShape.Polygon(floatArrayOf(25.9091f, 3.2727f, 30.3967f, 10.6364f, 21.4215f, 10.6364f, 25.9091f, 3.2727f), 0xFF9AA3B5),
        AvatarShape.Polygon(floatArrayOf(10.0909f, 6.5454f, 12.4528f, 10.2273f, 7.729f, 10.2273f, 10.0909f, 6.5454f), 0xFFC6CCD9),
        AvatarShape.Polygon(floatArrayOf(25.9091f, 6.5454f, 28.271f, 10.2273f, 23.5472f, 10.2273f, 25.9091f, 6.5454f), 0xFFC6CCD9),
        AvatarShape.Oval(18f, 19.6364f, 13.0909f, 13.0909f, 0xFF9AA3B5),
        AvatarShape.Oval(12.8182f, 18.5455f, 4.0909f, 4.3636f, 0xFFFFFFFF),
        AvatarShape.Oval(23.1818f, 18.5455f, 4.0909f, 4.3636f, 0xFFFFFFFF),
        AvatarShape.Oval(12.8182f, 19.3636f, 2.1818f, 2.4545f, 0xFF2D2C30),
        AvatarShape.Oval(23.1818f, 19.3636f, 2.1818f, 2.4545f, 0xFF2D2C30),
        AvatarShape.Oval(12.2727f, 18.5455f, 0.8182f, 0.8182f, 0xFFFFFFFF),
        AvatarShape.Oval(22.6364f, 18.5455f, 0.8182f, 0.8182f, 0xFFFFFFFF),
        AvatarShape.Oval(18f, 25.0909f, 5.1818f, 3.8182f, 0xFFF3F4F6),
        AvatarShape.Oval(18f, 23.1818f, 1.9091f, 1.3636f, 0xFF2D2C30),
        AvatarShape.Oval(18f, 26.4545f, 1.6364f, 1.3636f, 0xFF2D2C30),
        AvatarShape.Oval(7.9091f, 24.5455f, 2.7273f, 1.6364f, 0xFFC98B9B),
        AvatarShape.Oval(28.0909f, 24.5455f, 2.7273f, 1.6364f, 0xFFC98B9B)
    ),
    // 12: 공룡
    12 to listOf(
        AvatarShape.Oval(18f, 18f, 18f, 18f, 0xFFF3F4F6),
        AvatarShape.Polygon(floatArrayOf(10.3636f, 3.8182f, 14.1427f, 9.9545f, 6.5846f, 9.9545f, 10.3636f, 3.8182f), 0xFF3FA9A5),
        AvatarShape.Polygon(floatArrayOf(25.6364f, 3.8182f, 29.4154f, 9.9545f, 21.8573f, 9.9545f, 25.6364f, 3.8182f), 0xFF3FA9A5),
        AvatarShape.Oval(18f, 19.6364f, 13.0909f, 13.0909f, 0xFF57C7C2),
        AvatarShape.Oval(12.8182f, 18.5455f, 4.0909f, 4.3636f, 0xFFFFFFFF),
        AvatarShape.Oval(23.1818f, 18.5455f, 4.0909f, 4.3636f, 0xFFFFFFFF),
        AvatarShape.Oval(12.8182f, 19.3636f, 2.1818f, 2.4545f, 0xFF2D2C30),
        AvatarShape.Oval(23.1818f, 19.3636f, 2.1818f, 2.4545f, 0xFF2D2C30),
        AvatarShape.Oval(12.2727f, 18.5455f, 0.8182f, 0.8182f, 0xFFFFFFFF),
        AvatarShape.Oval(22.6364f, 18.5455f, 0.8182f, 0.8182f, 0xFFFFFFFF),
        AvatarShape.Oval(7.9091f, 24.5455f, 2.7273f, 1.6364f, 0xFF3FA9A5),
        AvatarShape.Oval(28.0909f, 24.5455f, 2.7273f, 1.6364f, 0xFF3FA9A5)
    )
)
