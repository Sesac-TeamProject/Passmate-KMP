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
import org.sesacteamproject.passmate.component.StudentAvatar
import org.sesacteamproject.passmate.component.StudentAvatars
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.navigation.NavigationAction
import org.sesacteamproject.passmate.preview.PassmatePreview
import org.sesacteamproject.passmate.theme.PassmateColors
import org.sesacteamproject.passmate.theme.PassmateTheme

// Figma "UI 디자인 v6" M-12-7(450:5938) — 내 캐릭터 변경. 4열 x 3행 그리드 + 선택 라벨 + 저장.
// 시안이 전체 페이지라 라우트 push로 띄운다 (규칙 §2-1 — 상세는 모달이 아니라 push)
@Composable
fun CharacterEditScreen(
    viewModel: CharacterEditViewModel = koinScreenViewModel(),
    onNavigate: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.onAction(CharacterEditAction.Enter)
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is CharacterEditEvent.Saved -> onNavigate(NavigationAction.NavigateBack)
                is CharacterEditEvent.ShowNotice -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        CharacterEditContentScreen(
            uiState = uiState,
            onAction = viewModel::onAction,
            onBack = { onNavigate(NavigationAction.NavigateBack) }
        )
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun CharacterEditContentScreen(
    uiState: CharacterEditUiState,
    onAction: (CharacterEditAction) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PassmateColors.Surface)
            // 배경은 상태바 뒤까지, 하단 인셋은 탭바(PassmateBottomTabBar)가 준다
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PassmateBackButton(onClick = onBack)
            Text(
                text = "내 캐릭터",
                color = PassmateColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp
            )
        }
        Text(
            text = "대기실 · 결과 화면에서 닉네임과 함께 보여요",
            color = PassmateColors.TextSecondary,
            fontSize = 14.sp,
            letterSpacing = (-0.28).sp
        )
        if (uiState.isLoading) {
            LoadingBox()
        } else if (uiState.hasLoadError) {
            RetryBox(onRetry = { onAction(CharacterEditAction.Retry) })
        } else {
            AvatarGrid(
                selectedId = uiState.avatarId,
                onSelect = { onAction(CharacterEditAction.SelectAvatar(it)) }
            )
            Text(
                text = "선택: ${StudentAvatars.nameOf(uiState.avatarId ?: StudentAvatars.DEFAULT_ID)}",
                color = PassmateColors.Primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.28).sp
            )
            SaveButton(
                enabled = uiState.canSubmit,
                isSubmitting = uiState.isSubmitting,
                onClick = { onAction(CharacterEditAction.Submit) }
            )
        }
    }
}

@Composable
private fun AvatarGrid(
    selectedId: Int?,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        (1..StudentAvatars.COUNT).chunked(AVATARS_PER_ROW).forEach { rowIds ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowIds.forEach { avatarId ->
                    val isSelected = avatarId == selectedId
                    val borderColor = if (isSelected) PassmateColors.Primary else PassmateColors.Border
                    val borderWidth = if (isSelected) 2.dp else 1.dp
                    val background = if (isSelected) PassmateColors.BackgroundMint else PassmateColors.Surface

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(84.dp)
                            .background(background, RoundedCornerShape(16.dp))
                            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
                            .clickable { onSelect(avatarId) },
                        contentAlignment = Alignment.Center
                    ) {
                        StudentAvatar(
                            avatarId = avatarId,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingBox() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = PassmateColors.Primary)
    }
}

@Composable
private fun RetryBox(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "캐릭터를 불러오지 못했어요",
            color = PassmateColors.TextSecondary,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "다시 시도",
            color = PassmateColors.Primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onRetry).padding(8.dp)
        )
    }
}

@Composable
private fun SaveButton(
    enabled: Boolean,
    isSubmitting: Boolean,
    onClick: () -> Unit
) {
    val background = if (enabled) PassmateColors.Primary else PassmateColors.Border

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(background, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSubmitting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = PassmateColors.Surface,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = "저장하기",
                color = PassmateColors.Surface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
        }
    }
}

private const val AVATARS_PER_ROW = 4

@PassmatePreview
@Composable
private fun CharacterEditPreview() {
    PassmateTheme {
        CharacterEditContentScreen(
            uiState = CharacterEditUiState(avatarId = 1, isLoading = false),
            onAction = {},
            onBack = {}
        )
    }
}
