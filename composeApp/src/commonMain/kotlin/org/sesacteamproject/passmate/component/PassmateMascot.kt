package org.sesacteamproject.passmate.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp

// 시안 프레임(120x132)과 공통 캔버스(144x156)의 관계 — 프레임은 캔버스 안 (16,24)에 놓인다.
// 상태 5개가 같은 캔버스를 쓰므로 상태를 바꿔도 몸통 크기·위치가 흔들리지 않는다.
// 리소스가 이 규격인지는 PassmateMascotResourceTest가 이 상수로 검사한다 — 값을 여기서만 고친다
internal const val MASCOT_FRAME_WIDTH = 120f
internal const val MASCOT_FRAME_HEIGHT = 132f
internal const val MASCOT_CANVAS_WIDTH = 144f
internal const val MASCOT_CANVAS_HEIGHT = 156f
internal const val MASCOT_BLEED_LEFT = 16f
internal const val MASCOT_BLEED_TOP = 24f

// 리소스 로딩 방식이 플랫폼마다 다르다 (Android는 R.drawable, Desktop은 classpath PNG)
@Composable
internal expect fun mascotPainter(mascot: PassmateMascots): Painter

// 마스코트 '패시' — 시안 컴포넌트 세트 Mascot(172:1037)에서 뽑은 PNG를 그린다.
//
// 레이아웃 박스는 시안 프레임(120x132 비율)이고, PASS 배지·컨페티·생각 방울 같은 장식은 그 밖으로
// 번진다. 시안 인스턴스도 오버플로를 자르지 않으므로(clipsContent=false) 같은 규격을 지킨다 —
// 그래서 리소스는 프레임보다 큰 공통 캔버스이고, 여기서 캔버스만큼 키운 뒤 번짐만큼 당겨 프레임을 맞춘다.
//
// 프레임 크기를 modifier가 아니라 파라미터로 받는 이유: 제약에서 되읽으면 부모가 제약을 주지 않을 때
// 무한대가 나와 requiredSize가 터진다(iOS 미러는 대신 부모를 꽉 채우며 조용히 커진다).
// 크기는 시안 인스턴스 크기를 그대로 준다 (예: M-02는 60x66).
//
// requiredSize를 쓰는 이유: size는 부모 제약(=프레임)을 넘지 못해 캔버스가 프레임 안으로
// 눌려 들어간다 — 마스코트가 120/144 만큼 작게, 번짐만큼 좌상단으로 밀려 그려진다.
//
// contentScale을 명시하는 이유: 기본값 Fit은 비율을 지켜 가운데 정렬하므로, 호출부 크기가
// 120:132에서 조금이라도 어긋나면 프레임 기준점이 밀린다. iOS의 .resizable()과 같게 맞춘다
@Composable
fun PassmateMascot(
    mascot: PassmateMascots,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = "패스메이트 마스코트 패시"
) {
    Box(modifier = modifier.size(width = width, height = height)) {
        Image(
            painter = mascotPainter(mascot),
            contentDescription = contentDescription,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .requiredSize(
                    width = width * (MASCOT_CANVAS_WIDTH / MASCOT_FRAME_WIDTH),
                    height = height * (MASCOT_CANVAS_HEIGHT / MASCOT_FRAME_HEIGHT)
                )
                .offset(
                    x = -width * (MASCOT_BLEED_LEFT / MASCOT_FRAME_WIDTH),
                    y = -height * (MASCOT_BLEED_TOP / MASCOT_FRAME_HEIGHT)
                )
        )
    }
}
