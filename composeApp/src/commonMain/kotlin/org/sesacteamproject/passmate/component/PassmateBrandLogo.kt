package org.sesacteamproject.passmate.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// 브랜드 심볼 (스플래시 M-00). 아이콘 체계(PassmateIcons)와 분리한 이유:
// 아이콘 리소스는 중립색만 담고 호출부에서 색을 준다는 규칙(§11-3, PassmateIconResourceTest가 강제)인데
// 이 심볼은 브랜드 색이 박힌 고정 에셋이라 그 계약을 만족하지 않는다.
// 리소스 파일은 tools/brand/BrandAssets.java가 생성한다 — 직접 고치지 말 것.
@Composable
expect fun PassmateBrandLogo(
    contentDescription: String?,
    modifier: Modifier = Modifier
)
