package org.sesacteamproject.passmate.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.theme.PassmateColors

// 공통 뒤로가기 버튼 — 상세 화면 헤더 좌측 (iOS PassmateBackButton.swift와 1:1)
// 레이아웃 점유는 24.dp 정사각으로 고정하고, 터치 영역만 44.dp로 넓힌다.
private const val BACK_GLYPH = "←"

private const val BACK_DESCRIPTION = "뒤로 가기"

@Composable
fun PassmateBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .requiredSize(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick, role = Role.Button)
                .semantics { contentDescription = BACK_DESCRIPTION },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = BACK_GLYPH,
                color = PassmateColors.TextPrimary,
                fontSize = 22.sp,
                modifier = Modifier.clearAndSetSemantics { }
            )
        }
    }
}
