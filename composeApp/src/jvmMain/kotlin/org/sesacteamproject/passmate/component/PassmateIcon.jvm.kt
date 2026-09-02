package org.sesacteamproject.passmate.component

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource

// Desktop은 classpath에서 VectorDrawable XML을 읽는다 (jvmMain/resources/drawable/*.xml).
// painterResource는 .xml을 loadXmlImageVector로 파싱한다 — 앱 아이콘(#36)과 같은 API
@Composable
actual fun PassmateIcon(
    icon: PassmateIcons,
    contentDescription: String?,
    modifier: Modifier,
    tint: Color
) {
    Icon(
        painter = painterResource("drawable/${icon.resourceName}.xml"),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}
