package org.sesacteamproject.passmate.component

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import org.sesacteamproject.passmate.R

// R.drawable 경로를 쓰는 이유는 PassmateIcon.android.kt와 같다 — 프리뷰 렌더러가 classpath를 못 찾는다
@Composable
actual fun PassmateBrandMark(
    contentDescription: String?,
    modifier: Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.ic_brand_mark),
        contentDescription = contentDescription,
        modifier = modifier
    )
}
