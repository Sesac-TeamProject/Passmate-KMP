package org.sesacteamproject.passmate.ui.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.theme.PassmateColors

// Figma "UI 디자인 v6" M-12-10(450:6326) — 알림 설정 3종(세션 시작·별점 요청·정산 완료), 토글 즉시 저장.
// 시트 표시 여부는 호스팅 화면(SettingsScreen)이 소유한다 (규칙 §11-1)
@Composable
fun NotificationSettingsSheet(
    onNotice: (String) -> Unit,
    onClose: () -> Unit
) {
    val viewModel: NotificationSettingsViewModel = koinScreenViewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onAction(NotificationSettingsAction.Enter)
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is NotificationSettingsEvent.ShowNotice -> onNotice(event.message)
            }
        }
    }
    NotificationSettingsContentView(
        uiState = uiState,
        onAction = viewModel::onAction,
        onClose = onClose
    )
}

@Composable
private fun NotificationSettingsContentView(
    uiState: NotificationSettingsUiState,
    onAction: (NotificationSettingsAction) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PassmateColors.Surface)
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "알림 설정",
                color = PassmateColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp
            )
            Text(
                text = "✕",
                color = PassmateColors.TextSecondary,
                fontSize = 18.sp,
                modifier = Modifier
                    .clickable(onClick = onClose)
                    .padding(4.dp)
            )
        }
        when {
            uiState.isLoading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PassmateColors.Primary)
            }
            uiState.loadFailed -> Text(
                text = "설정을 불러오지 못했어요 · 다시 시도",
                color = PassmateColors.WeakTopicText,
                fontSize = 14.sp,
                letterSpacing = (-0.28).sp,
                modifier = Modifier
                    .clickable { onAction(NotificationSettingsAction.Retry) }
                    .padding(vertical = 24.dp)
            )
            else -> {
                ToggleRow(
                    title = "세션 시작",
                    subtitle = "참여한 방의 세션이 시작되면 알려드려요",
                    checked = uiState.sessionStart,
                    enabled = !uiState.isSaving,
                    onToggle = { onAction(NotificationSettingsAction.Toggle(NotificationKind.SESSION_START)) }
                )
                ToggleRow(
                    title = "별점 요청",
                    subtitle = "세션 종료 후 평가 요청을 알려드려요",
                    checked = uiState.ratingRequest,
                    enabled = !uiState.isSaving,
                    onToggle = { onAction(NotificationSettingsAction.Toggle(NotificationKind.RATING_REQUEST)) }
                )
                ToggleRow(
                    title = "정산 완료",
                    subtitle = "유료 방 정산이 지급되면 알려드려요",
                    checked = uiState.settlementDone,
                    enabled = !uiState.isSaving,
                    onToggle = { onAction(NotificationSettingsAction.Toggle(NotificationKind.SETTLEMENT_DONE)) }
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                color = PassmateColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.3).sp
            )
            Text(
                text = subtitle,
                color = PassmateColors.TextTertiary,
                fontSize = 12.sp,
                letterSpacing = (-0.24).sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = PassmateColors.Primary,
                checkedThumbColor = PassmateColors.Surface
            )
        )
    }
}
