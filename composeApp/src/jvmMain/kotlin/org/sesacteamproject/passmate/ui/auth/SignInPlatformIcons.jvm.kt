package org.sesacteamproject.passmate.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Composable
internal actual fun GoogleSignInIcon(modifier: Modifier) {
    Image(
        imageVector = googleSignInIconVector,
        contentDescription = null,
        modifier = modifier
    )
}

@Composable
internal actual fun AppleSignInIcon(modifier: Modifier) {
    Image(
        imageVector = appleSignInIconVector,
        contentDescription = null,
        modifier = modifier
    )
}

private val googleSignInIconVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "GoogleSignInIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color(0xFFE5E7EB)),
            strokeLineWidth = 1f
        ) {
            // 24x24 뷰포트에 반지름 11.5 원 테두리 — PathBuilder에는 addRoundRect가 없어 반원 호 2개로 그린다
            // (원본 수치 left/top 0.5·right/bottom 23.5·radius 11.5 = 중심 (12,12) 반지름 11.5의 원과 동일)
            moveTo(0.5f, 12f)
            arcTo(11.5f, 11.5f, 0f, true, true, 23.5f, 12f)
            arcTo(11.5f, 11.5f, 0f, true, true, 0.5f, 12f)
            close()
        }
        path(fill = SolidColor(Color(0xFFEA4335)), pathFillType = PathFillType.NonZero) {
            moveTo(12f, 7.16667f)
            curveTo(13.18f, 7.16667f, 14.2367f, 7.57333f, 15.07f, 8.36667f)
            lineTo(17.3533f, 6.08333f)
            curveTo(15.9667f, 4.79333f, 14.1567f, 4f, 12f, 4f)
            curveTo(8.87333f, 4f, 6.17f, 5.79333f, 4.85333f, 8.40667f)
            lineTo(7.51333f, 10.47f)
            curveTo(8.14333f, 8.57333f, 9.91333f, 7.16667f, 12f, 7.16667f)
            close()
        }
        path(fill = SolidColor(Color(0xFF4285F4)), pathFillType = PathFillType.NonZero) {
            moveTo(19.66f, 12.1827f)
            curveTo(19.66f, 11.6593f, 19.61f, 11.1527f, 19.5333f, 10.666f)
            lineTo(12f, 10.666f)
            lineTo(12f, 13.6727f)
            lineTo(16.3133f, 13.6727f)
            curveTo(16.12f, 14.6593f, 15.56f, 15.4993f, 14.72f, 16.066f)
            lineTo(17.2967f, 18.066f)
            curveTo(18.8f, 16.6727f, 19.66f, 14.6127f, 19.66f, 12.1827f)
            close()
        }
        path(fill = SolidColor(Color(0xFFFBBC05)), pathFillType = PathFillType.NonZero) {
            moveTo(7.51f, 13.5306f)
            curveTo(7.35f, 13.0472f, 7.25667f, 12.5339f, 7.25667f, 12.0006f)
            curveTo(7.25667f, 11.4672f, 7.34667f, 10.9539f, 7.51f, 10.4706f)
            lineTo(4.85f, 8.40723f)
            curveTo(4.30667f, 9.48723f, 4f, 10.7072f, 4f, 12.0006f)
            curveTo(4f, 13.2939f, 4.30667f, 14.5139f, 4.85333f, 15.5939f)
            lineTo(7.51f, 13.5306f)
            close()
        }
        path(fill = SolidColor(Color(0xFF34A853)), pathFillType = PathFillType.NonZero) {
            moveTo(12f, 19.9997f)
            curveTo(14.16f, 19.9997f, 15.9767f, 19.2897f, 17.2967f, 18.063f)
            lineTo(14.72f, 16.063f)
            curveTo(14.0033f, 16.5464f, 13.08f, 16.8297f, 12f, 16.8297f)
            curveTo(9.91334f, 16.8297f, 8.14334f, 15.423f, 7.51f, 13.5264f)
            lineTo(4.85001f, 15.5897f)
            curveTo(6.17001f, 18.2064f, 8.87334f, 19.9997f, 12f, 19.9997f)
            close()
        }
    }.build()
}

private val appleSignInIconVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "AppleSignInIcon",
        defaultWidth = 20.dp,
        defaultHeight = 20.dp,
        viewportWidth = 20f,
        viewportHeight = 20f
    ).apply {
        path(
            fill = SolidColor(Color.White),
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(14.2083f, 16.9f)
            curveTo(13.3917f, 17.6917f, 12.5f, 17.5667f, 11.6417f, 17.1917f)
            curveTo(10.7333f, 16.8083f, 9.89999f, 16.7917f, 8.94166f, 17.1917f)
            curveTo(7.74166f, 17.7083f, 7.10833f, 17.5583f, 6.39166f, 16.9f)
            curveTo(2.32499f, 12.7083f, 2.92499f, 6.325f, 7.54166f, 6.09167f)
            curveTo(8.66666f, 6.15f, 9.44999f, 6.70833f, 10.1083f, 6.75833f)
            curveTo(11.0917f, 6.55833f, 12.0333f, 5.98333f, 13.0833f, 6.05833f)
            curveTo(14.3417f, 6.15833f, 15.2917f, 6.65833f, 15.9167f, 7.55833f)
            curveTo(13.3167f, 9.11667f, 13.9333f, 12.5417f, 16.3167f, 13.5f)
            curveTo(15.8417f, 14.75f, 15.225f, 15.9917f, 14.2f, 16.9083f)
            lineTo(14.2083f, 16.9f)
            close()
            moveTo(10.025f, 6.04167f)
            curveTo(9.89999f, 4.18333f, 11.4083f, 2.65f, 13.1417f, 2.5f)
            curveTo(13.3833f, 4.65f, 11.1917f, 6.25f, 10.025f, 6.04167f)
            close()
        }
    }.build()
}
