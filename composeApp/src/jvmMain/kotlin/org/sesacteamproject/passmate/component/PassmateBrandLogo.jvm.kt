package org.sesacteamproject.passmate.component

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource

// Desktop은 classpath에서 VectorDrawable XML을 읽는다 (jvmMain/resources/drawable/*.xml)
@Composable
actual fun PassmateBrandLogo(
    contentDescription: String?,
    modifier: Modifier
) {
    Image(
        painter = painterResource("drawable/ic_splash_logo.xml"),
        contentDescription = contentDescription,
        modifier = modifier
    )
}
