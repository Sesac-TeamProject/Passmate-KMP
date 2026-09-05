package org.sesacteamproject.passmate.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource

// Desktop은 classpath에서 PNG를 읽는다 (jvmMain/resources/drawable/*.png)
@Composable
internal actual fun mascotPainter(mascot: PassmateMascots): Painter {
    return painterResource("drawable/${mascot.resourceName}.png")
}
