package org.sesacteamproject.passmate.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// 브랜드 마크 (시안 '11 · 브랜드 — 로고' > 심볼 단독 · 민트 배경 위) — 로고 락업에 쓰는
// 둥근 사각형 형태다(민트 바탕 + 흰 심볼, 반경 = 한 변의 22.4% · 심볼 높이 62%).
// PassmateBrandLogo(스플래시)는 배경 없는 심볼 단독이라 서로 다른 에셋이다.
// 아이콘 체계(PassmateIcons)와 분리한 이유는 PassmateBrandLogo와 같다 — 브랜드 2색이 박힌 고정 에셋이다.
// 리소스 파일은 tools/brand/BrandAssets.java가 생성한다 — 직접 고치지 말 것.
@Composable
expect fun PassmateBrandMark(
    contentDescription: String?,
    modifier: Modifier = Modifier
)
