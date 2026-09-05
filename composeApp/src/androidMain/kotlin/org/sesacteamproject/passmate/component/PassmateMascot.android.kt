package org.sesacteamproject.passmate.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import org.sesacteamproject.passmate.R

// R.drawable을 쓰는 이유는 아이콘과 같다 — 클래스패스에서 읽는 방식은 안드로이드 스튜디오
// 프리뷰 렌더러가 찾지 못한다(compose-multiplatform #4476). 이 리포는 프리뷰로 시안을 대조한다.
// 파일은 drawable-xxxhdpi(4x) 하나만 둔다. 그보다 낮은 밀도는 안드로이드가 축소해 쓴다
private fun PassmateMascots.drawableId(): Int {
    return when (this) {
        PassmateMascots.Default -> R.drawable.img_mascot_default
        PassmateMascots.Enter -> R.drawable.img_mascot_enter
        PassmateMascots.Waiting -> R.drawable.img_mascot_waiting
        PassmateMascots.Success -> R.drawable.img_mascot_success
        PassmateMascots.Feedback -> R.drawable.img_mascot_feedback
    }
}

@Composable
internal actual fun mascotPainter(mascot: PassmateMascots): Painter {
    return painterResource(id = mascot.drawableId())
}
