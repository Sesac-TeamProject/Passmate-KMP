package org.sesacteamproject.passmate.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.theme.PassmateColors

// 공통 상단 앱바 — 시안 `header` 프레임 기준. 확인한 화면 전부가 같은 규격이다:
// M-12 마이(349:9684) · M-09 명성(349:9771) · M-T4 정산(349:10200) · M-12-1 계정 정보(437:5425) · M-14 방 리포트(432:5367).
// 구성은 [뒤로가기 24] gap12 [타이틀] spacer [우측 액션], 좌우 20 · 하단 16.
//
// 여백은 컴포넌트가 고정한다 — 화면마다 달라지면 시안과 어긋나기 때문이다.
// 앱바는 스크롤 컨테이너 **밖**에 두고, 좌우 여백은 본문이 따로 준다
// (`Column { PassmateTopBar(...); Body(Modifier.verticalScroll(...).padding(horizontal = 20.dp)) }`).
enum class PassmateTopBarStyle(
    internal val fontSize: TextUnit,
    internal val letterSpacing: TextUnit
) {
    // 탭 루트·명성·정산 — 시안 24px (헤더 타이틀 높이 29 = 24 x 1.2)
    Root(24.sp, (-0.48).sp),

    // 그 밖의 상세 화면 — 시안 heading-md 20px (높이 24 = 20 x 1.2)
    Detail(20.sp, (-0.4).sp)
}

private val HORIZONTAL_PADDING = 20.dp

// 시안은 타이틀이 화면 최상단에서 y=56이다. 상태바(iPhone 47)를 statusBarsPadding이 이미 먹으므로
// 그 아래 여백만 남긴다 — 56 - 47 = 9에 가까운 12로 전 화면을 통일한다
private val TOP_PADDING = 12.dp

private val BOTTOM_PADDING = 16.dp

private val CONTENT_GAP = 12.dp

@Composable
fun PassmateTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    style: PassmateTopBarStyle = PassmateTopBarStyle.Detail,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = HORIZONTAL_PADDING,
                end = HORIZONTAL_PADDING,
                top = TOP_PADDING,
                bottom = BOTTOM_PADDING
            ),
        horizontalArrangement = Arrangement.spacedBy(CONTENT_GAP),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            PassmateBackButton(onClick = onBack)
        }
        Text(
            text = title,
            color = PassmateColors.TextPrimary,
            fontSize = style.fontSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = style.letterSpacing
        )
        Spacer(modifier = Modifier.weight(1f))
        trailing()
    }
}
