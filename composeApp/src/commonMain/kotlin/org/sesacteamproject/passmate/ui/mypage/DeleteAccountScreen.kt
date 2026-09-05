package org.sesacteamproject.passmate.ui.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import org.sesacteamproject.passmate.component.PassmateBackButton
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.navigation.NavigationAction
import org.sesacteamproject.passmate.preview.PassmatePreview
import org.sesacteamproject.passmate.theme.PassmateColors
import org.sesacteamproject.passmate.theme.PassmateTheme

// 회원 탈퇴 (M-12-12) — 삭제 대상 안내 + "위 내용을 확인했어요" 체크 후에만 탈퇴.
// 설정에서 push로 진입한다 (이전에는 설정 안의 확인 다이얼로그였다)
@Composable
fun DeleteAccountScreen(
    viewModel: DeleteAccountViewModel = koinScreenViewModel(),
    onNavigate: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.onAction(DeleteAccountAction.Enter)
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is DeleteAccountEvent.Deleted -> onNavigate(NavigationAction.NavigateToHome)
                is DeleteAccountEvent.ShowNotice -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        DeleteAccountContentScreen(
            uiState = uiState,
            onAction = viewModel::onAction,
            onBack = { onNavigate(NavigationAction.NavigateBack) }
        )
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun DeleteAccountContentScreen(
    uiState: DeleteAccountUiState,
    onAction: (DeleteAccountAction) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PassmateColors.Surface)
            // 배경은 상태바 뒤까지, 하단 인셋은 탭바(PassmateBottomTabBar)가 준다 — 시안이 이 화면에 탭바를 유지한다
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        TopBar(onBack = onBack)
        Spacer(Modifier.height(20.dp))
        DeletionNoticeCard(coins = uiState.coins)
        Spacer(Modifier.height(14.dp))
        Text(
            text = "정산 예정 금액이 있으면 지급이 끝난 뒤 탈퇴할 수 있어요.",
            color = PassmateColors.TextTertiary,
            fontSize = 12.sp,
            letterSpacing = (-0.24).sp
        )
        Spacer(Modifier.height(16.dp))
        ConfirmRow(
            isConfirmed = uiState.isConfirmed,
            onToggle = { onAction(DeleteAccountAction.ToggleConfirm) }
        )
        Spacer(Modifier.weight(1f))
        DeleteButton(uiState = uiState, onClick = { onAction(DeleteAccountAction.ClickDelete) })
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PassmateBackButton(onClick = onBack)
        Text(
            text = "회원 탈퇴",
            color = PassmateColors.TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp
        )
    }
}

@Composable
private fun DeletionNoticeCard(coins: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PassmateColors.FieldGray, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "탈퇴하면 아래 내용이 모두 삭제돼요",
            color = PassmateColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp
        )
        BulletLine(text = "참여 기록 · 뱃지 · 명성 등급")
        BulletLine(text = "보유 코인 ${formatNumber(coins)} C (환불되지 않아요)")
        BulletLine(text = "내가 만든 방 · 문제 세트")
    }
}

@Composable
private fun BulletLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(4.dp).background(PassmateColors.TextTertiary, CircleShape))
        Spacer(Modifier.size(10.dp))
        Text(
            text = text,
            color = PassmateColors.TextSecondary,
            fontSize = 14.sp,
            letterSpacing = (-0.28).sp
        )
    }
}

@Composable
private fun ConfirmRow(
    isConfirmed: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.clickable { onToggle() }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheckMark(isConfirmed = isConfirmed)
        Spacer(Modifier.size(10.dp))
        Text(
            text = "위 내용을 확인했어요",
            color = PassmateColors.TextPrimary,
            fontSize = 15.sp,
            letterSpacing = (-0.3).sp
        )
    }
}

@Composable
private fun CheckMark(isConfirmed: Boolean) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(
                color = if (isConfirmed) PassmateColors.Primary else PassmateColors.Surface,
                shape = RoundedCornerShape(7.dp)
            )
            .border(
                width = if (isConfirmed) 0.dp else 1.5.dp,
                color = if (isConfirmed) PassmateColors.Primary else PassmateColors.Border,
                shape = RoundedCornerShape(7.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isConfirmed) {
            Text("✓", color = PassmateColors.Surface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// 파괴적 동작이라 시안이 Primary 대신 검정 계열을 쓴다 — 토큰 중 TextPrimary가 그 톤이다 (규칙 §11-2)
@Composable
private fun DeleteButton(
    uiState: DeleteAccountUiState,
    onClick: () -> Unit
) {
    val enabled = uiState.canDelete
    val bg = if (enabled) PassmateColors.TextPrimary else PassmateColors.Border

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        if (uiState.isProcessing) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
                color = PassmateColors.Surface
            )
        } else {
            Text("탈퇴하기", color = PassmateColors.Surface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatNumber(value: Int): String {
    return value.toString().reversed().chunked(3).joinToString(",").reversed()
}

// --- Preview ---

// 확인 체크 완료 — 탈퇴 버튼 활성
@PassmatePreview
@Composable
private fun DeleteAccountContentScreenPreview() {
    PassmateTheme {
        DeleteAccountContentScreen(
            uiState = DeleteAccountUiState(isLoading = false, coins = 1200, isConfirmed = true),
            onAction = {},
            onBack = {}
        )
    }
}

// 미체크 — 탈퇴 버튼 비활성 (게이트는 canDelete가 판단)
@PassmatePreview
@Composable
private fun DeleteAccountContentScreenUncheckedPreview() {
    PassmateTheme {
        DeleteAccountContentScreen(
            uiState = DeleteAccountUiState(isLoading = false, coins = 1200, isConfirmed = false),
            onAction = {},
            onBack = {}
        )
    }
}

// 탈퇴 요청 in-flight — 중복 호출 방지로 버튼 비활성 (규칙 §9)
@PassmatePreview
@Composable
private fun DeleteAccountContentScreenProcessingPreview() {
    PassmateTheme {
        DeleteAccountContentScreen(
            uiState = DeleteAccountUiState(
                isLoading = false,
                coins = 1200,
                isConfirmed = true,
                isProcessing = true
            ),
            onAction = {},
            onBack = {}
        )
    }
}
