package org.sesacteamproject.passmate.component

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

// 공통 아이콘 — 리소스 로딩 방식이 플랫폼마다 다르다(Android는 R.drawable, Desktop은 classpath XML).
// 파라미터 순서는 Material3 Icon과 같게 맞춘다. 색은 리소스가 아니라 호출부 토큰으로 준다 (규칙 §11-2)
@Composable
expect fun PassmateIcon(
    icon: PassmateIcons,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
)
