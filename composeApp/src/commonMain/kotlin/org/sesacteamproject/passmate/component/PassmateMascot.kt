package org.sesacteamproject.passmate.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter

// 시안 프레임(120x132)과 공통 캔버스(144x156)의 관계 — 프레임은 캔버스 안 (16,24)에 놓인다.
// 상태 5개가 같은 캔버스를 쓰므로 상태를 바꿔도 몸통 크기·위치가 흔들리지 않는다
private const val FRAME_WIDTH = 120f
private const val FRAME_HEIGHT = 132f
private const val CANVAS_WIDTH = 144f
private const val CANVAS_HEIGHT = 156f
private const val BLEED_LEFT = 16f
private const val BLEED_TOP = 24f

// 리소스 로딩 방식이 플랫폼마다 다르다 (Android는 R.drawable, Desktop은 classpath PNG)
@Composable
internal expect fun mascotPainter(mascot: PassmateMascots): Painter

// 마스코트 '패시' — 시안 컴포넌트 세트 Mascot(172:1037)에서 뽑은 PNG를 그린다.
//
// 레이아웃 박스는 시안 프레임(120x132)이고, PASS 배지·컨페티·생각 방울 같은 장식은 그 밖으로 번진다.
// 시안 인스턴스도 오버플로를 자르지 않으므로(clipsContent=false) 같은 규격을 지킨다 — 그래서 리소스는
// 프레임보다 큰 공통 캔버스이고, 여기서 캔버스만큼 키운 뒤 번짐만큼 왼쪽·위로 당겨 프레임을 맞춘다.
//
// requiredSize를 쓰는 이유: size는 부모 제약(=프레임)을 넘지 못해 캔버스가 프레임 안으로
// 눌려 들어간다 — 마스코트가 120/144 만큼 작게, 번짐만큼 좌상단으로 밀려 그려진다.
//
// 호출부는 modifier로 프레임 크기를 준다(시안 인스턴스 크기 그대로 — 예: M-02는 60x66).
// 크기를 주지 않으면 부모 제약을 프레임으로 삼으므로 반드시 size를 지정한다
@Composable
fun PassmateMascot(
    mascot: PassmateMascots,
    modifier: Modifier = Modifier,
    contentDescription: String? = "패스메이트 마스코트 패시"
) {
    BoxWithConstraints(modifier = modifier) {
        val frameWidth = maxWidth
        val frameHeight = maxHeight

        Image(
            painter = mascotPainter(mascot),
            contentDescription = contentDescription,
            modifier = Modifier
                .requiredSize(
                    width = frameWidth * (CANVAS_WIDTH / FRAME_WIDTH),
                    height = frameHeight * (CANVAS_HEIGHT / FRAME_HEIGHT)
                )
                .offset(
                    x = -frameWidth * (BLEED_LEFT / FRAME_WIDTH),
                    y = -frameHeight * (BLEED_TOP / FRAME_HEIGHT)
                )
        )
    }
}
