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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.navigation.NavigationAction
import org.sesacteamproject.passmate.theme.PassmateColors

// 설정 — 마이 탭 우상단 "설정"에서 push. 회원 탈퇴(M-12-12, 확인 다이얼로그)만 둔다
@Composable
fun SettingsScreen(onNavigate: (NavigationAction) -> Unit) {
    val viewModel: SettingsViewModel = koinScreenViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.onAction(SettingsAction.Enter)
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is SettingsEvent.RequireSignIn -> onNavigate(NavigationAction.NavigateToSignIn())
                is SettingsEvent.AccountDeleted -> onNavigate(NavigationAction.NavigateToHome)
                is SettingsEvent.ShowNotice -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        SettingsContentScreen(
            uiState = uiState,
            onClickBack = { onNavigate(NavigationAction.NavigateBack) },
            onClickDelete = { showDeleteConfirm = true }
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = "회원 탈퇴",
                    color = PassmateColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "탈퇴하면 참여·개설 기록과 보유 코인이 모두 삭제되고 되돌릴 수 없어요. 정산 대기 금액이나 진행 중인 방이 있으면 탈퇴할 수 없어요.",
                    color = PassmateColors.TextSecondary,
                    fontSize = 14.sp,
                    letterSpacing = (-0.28).sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.onAction(SettingsAction.ConfirmDeleteAccount)
                    }
                ) {
                    Text(text = "탈퇴", color = PassmateColors.WeakTopicText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(text = "취소", color = PassmateColors.TextSecondary)
                }
            },
            containerColor = PassmateColors.Surface
        )
    }
}

@Composable
private fun SettingsContentScreen(
    uiState: SettingsUiState,
    onClickBack: () -> Unit,
    onClickDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PassmateColors.Surface)
            .padding(start = 20.dp, top = 60.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "설정",
                color = PassmateColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.48).sp
            )
            Text(
                text = "닫기",
                color = PassmateColors.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp,
                modifier = Modifier
                    .clickable(onClick = onClickBack)
                    .padding(4.dp)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, PassmateColors.Border, RoundedCornerShape(16.dp))
                .background(PassmateColors.Surface, RoundedCornerShape(16.dp))
                .clickable(onClick = onClickDelete)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "회원 탈퇴",
                color = PassmateColors.WeakTopicText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(text = "›", color = PassmateColors.TextTertiary, fontSize = 18.sp)
        }
        if (uiState.isProcessing) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = PassmateColors.Primary,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}
