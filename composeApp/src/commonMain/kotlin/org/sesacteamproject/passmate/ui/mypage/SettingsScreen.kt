package org.sesacteamproject.passmate.ui.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.navigation.NavigationAction
import org.sesacteamproject.passmate.preview.PassmatePreview
import org.sesacteamproject.passmate.theme.PassmateColors
import org.sesacteamproject.passmate.theme.PassmateTheme

// 설정 — 마이 탭 우상단 "설정"에서 push. 회원 탈퇴는 전용 화면(M-12-12)으로 push한다
@Composable
fun SettingsScreen(onNavigate: (NavigationAction) -> Unit) {
    val viewModel: SettingsViewModel = koinScreenViewModel()

    LaunchedEffect(Unit) {
        viewModel.onAction(SettingsAction.Enter)
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is SettingsEvent.RequireSignIn -> onNavigate(
                    NavigationAction.NavigateToSignIn(NavigationAction.NavigateToSettings)
                )
            }
        }
    }
    SettingsContentScreen(
        onClickBack = { onNavigate(NavigationAction.NavigateBack) },
        onClickDelete = { onNavigate(NavigationAction.NavigateToDeleteAccount) }
    )
}

@Composable
private fun SettingsContentScreen(
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
    }
}

// --- Preview ---

@PassmatePreview
@Composable
private fun SettingsContentScreenPreview() {
    PassmateTheme {
        SettingsContentScreen(
            uiState = SettingsUiState(isProcessing = false),
            onClickBack = {},
            onClickDelete = {}
        )
    }
}

// 탈퇴 요청 in-flight — 하단 스피너
@PassmatePreview
@Composable
private fun SettingsContentScreenProcessingPreview() {
    PassmateTheme {
        SettingsContentScreen(
            uiState = SettingsUiState(isProcessing = true),
            onClickBack = {},
            onClickDelete = {}
        )
    }
}
