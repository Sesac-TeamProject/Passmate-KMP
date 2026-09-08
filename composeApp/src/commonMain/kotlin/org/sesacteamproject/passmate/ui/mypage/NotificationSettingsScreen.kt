package org.sesacteamproject.passmate.ui.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.component.PassmateTopBar
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.navigation.NavigationAction
import org.sesacteamproject.passmate.theme.PassmateColors

// Figma "UI 디자인 v6" M-12-10(450:6326) — 알림 설정 3종(세션 시작·별점 요청·정산 완료), 토글 즉시 저장.
// 시안이 전체 페이지라 라우트 push로 띄운다 (규칙 §2-1 — 상세는 모달이 아니라 push)
@Composable
fun NotificationSettingsScreen(
    viewModel: NotificationSettingsViewModel = koinScreenViewModel(),
    onNavigate: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.onAction(NotificationSettingsAction.Enter)
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is NotificationSettingsEvent.ShowNotice -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        NotificationSettingsContentScreen(
            uiState = uiState,
            onAction = viewModel::onAction,
            onBack = { onNavigate(NavigationAction.NavigateBack) }
        )
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun NotificationSettingsContentScreen(
    uiState: NotificationSettingsUiState,
    onAction: (NotificationSettingsAction) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PassmateColors.Surface)
            // 배경은 상태바 뒤까지, 하단 인셋은 탭바(PassmateBottomTabBar)가 준다
            .statusBarsPadding()
    ) {
        // 앱바는 여백을 스스로 가진다 — 본문 여백은 아래 Column이 따로 준다
        PassmateTopBar(
            title = "알림 설정",
            onBack = onBack
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
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
                // 시안 M-12-10 — 토글 행들은 테두리 카드(r16) 안에 구분선으로 나뉜다
                else -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .border(1.dp, PassmateColors.Border, RoundedCornerShape(16.dp))
                ) {
                    ToggleRow(
                        title = "세션 시작",
                        subtitle = "참여한 방이 시작되면 알려줘요",
                        checked = uiState.sessionStart,
                        enabled = !uiState.isSaving,
                        onToggle = { onAction(NotificationSettingsAction.Toggle(NotificationKind.SESSION_START)) }
                    )
                    Divider(color = PassmateColors.Border, thickness = 1.dp)
                    ToggleRow(
                        title = "별점 요청",
                        subtitle = "방이 끝나면 선생님 평가를 요청해요",
                        checked = uiState.ratingRequest,
                        enabled = !uiState.isSaving,
                        onToggle = { onAction(NotificationSettingsAction.Toggle(NotificationKind.RATING_REQUEST)) }
                    )
                    Divider(color = PassmateColors.Border, thickness = 1.dp)
                    ToggleRow(
                        title = "정산 완료",
                        subtitle = "정산금이 입금되면 알려줘요",
                        checked = uiState.settlementDone,
                        enabled = !uiState.isSaving,
                        onToggle = { onAction(NotificationSettingsAction.Toggle(NotificationKind.SETTLEMENT_DONE)) }
                    )
                }
            }
            // 시안 M-12-10 각주 — 앱 설정만으로는 못 켜는 경우를 안내한다
            Text(
                text = "기기 알림이 꺼져 있으면 휴대폰 설정에서 패스메이트 알림을 켜주세요.",
                color = PassmateColors.TextTertiary,
                fontSize = 12.sp,
                letterSpacing = (-0.24).sp,
                modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp)
            )
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
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
