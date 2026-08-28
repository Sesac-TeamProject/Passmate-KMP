package org.sesacteamproject.passmate.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.sesacteamproject.passmate.theme.PassmateColors

// v6 공통 카드 — 흰 배경 + 보더 + 라운드 24 (M-01 입장 카드·M-02 대기실 카드)
@Composable
fun PassmateCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(24.dp))
            .background(PassmateColors.Surface, RoundedCornerShape(24.dp)),
        content = content
    )
}
