package org.sesacteamproject.passmate.component

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import org.sesacteamproject.passmate.R

// R.drawable 경로를 쓰는 이유: 클래스패스에서 읽는 방식은 안드로이드 스튜디오 프리뷰 렌더러가
// 찾지 못한다(compose-multiplatform #4476). 이 리포는 프리뷰로 시안을 대조하므로 프리뷰가 그려져야 한다
private fun PassmateIcons.drawableId(): Int {
    return when (this) {
        PassmateIcons.DoorOpen -> R.drawable.ic_door_open
        PassmateIcons.Bookmark -> R.drawable.ic_bookmark
        PassmateIcons.AlertCircle -> R.drawable.ic_alert_circle
    }
}

@Composable
actual fun PassmateIcon(
    icon: PassmateIcons,
    contentDescription: String?,
    modifier: Modifier,
    tint: Color
) {
    Icon(
        painter = painterResource(id = icon.drawableId()),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}
