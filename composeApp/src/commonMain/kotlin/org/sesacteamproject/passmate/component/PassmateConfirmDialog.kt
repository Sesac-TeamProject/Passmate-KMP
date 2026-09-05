package org.sesacteamproject.passmate.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.sesacteamproject.passmate.theme.PassmateColors

// 시안 v6 확인 다이얼로그 (M-12-11 로그아웃 확인 등) — 플랫폼 기본 알림 대신 쓴다.
// 제목·본문 가운데 정렬, 취소(테두리)와 확인(채움)을 같은 폭으로 나란히 둔다.
// 표시 여부와 생명주기는 호출한 화면이 소유한다 (규칙 §11-1)
@Composable
fun PassmateConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = "취소"
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PassmateColors.Surface, RoundedCornerShape(24.dp))
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                color = PassmateColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                color = PassmateColors.TextSecondary,
                fontSize = 15.sp,
                letterSpacing = (-0.3).sp,
                textAlign = TextAlign.Center
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DialogButton(
                    label = dismissLabel,
                    isPrimary = false,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                DialogButton(
                    label = confirmLabel,
                    isPrimary = true,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DialogButton(
    label: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = if (isPrimary) PassmateColors.Primary else PassmateColors.Surface
    val textColor = if (isPrimary) PassmateColors.Surface else PassmateColors.TextPrimary

    Box(
        modifier = modifier
            .height(52.dp)
            .background(background, RoundedCornerShape(16.dp))
            .border(
                width = if (isPrimary) 0.dp else 1.dp,
                color = if (isPrimary) PassmateColors.Primary else PassmateColors.Border,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp
        )
    }
}
